import boto3
import time
import sys

REGION = "us-east-1"
PROJECT_TAG = "caribexperience"

print(f"=== Iniciando destrucción de recursos de '{PROJECT_TAG}' en {REGION} ===")

session = boto3.Session(region_name=REGION)
ec2 = session.client("ec2")
elbv2 = session.client("elbv2")
asg = session.client("autoscaling")
s3 = session.client("s3")
dynamodb = session.client("dynamodb")

# 1. AUTO SCALING GROUPS
print("\n[1/9] Buscando y eliminando Auto Scaling Groups...")
try:
    asgs = asg.describe_auto_scaling_groups()["AutoScalingGroups"]
    for group in asgs:
        name = group["AutoScalingGroupName"]
        if PROJECT_TAG in name.lower():
            print(f"  -> Eliminando ASG: {name}")
            try:
                asg.update_auto_scaling_group(
                    AutoScalingGroupName=name,
                    MinSize=0,
                    MaxSize=0,
                    DesiredCapacity=0
                )
                asg.delete_auto_scaling_group(AutoScalingGroupName=name, ForceDelete=True)
                print(f"     ASG {name} marcado para eliminación forzada.")
            except Exception as e:
                print(f"     Error eliminando ASG {name}: {e}")
except Exception as e:
    print(f"Error consultando ASGs: {e}")

# 2. INSTANCIAS EC2
print("\n[2/9] Esperando terminación de instancias EC2 asociadas...")
try:
    instances = ec2.describe_instances(
        Filters=[
            {"Name": "instance-state-name", "Values": ["pending", "running", "shutting-down", "stopping", "stopped"]},
            {"Name": "tag:Project", "Values": [PROJECT_TAG]}
        ]
    )["Reservations"]
    inst_ids = [i["InstanceId"] for r in instances for i in r["Instances"]]
    
    # También buscar por tag Name si no tenían tag Project
    more_inst = ec2.describe_instances(
        Filters=[
            {"Name": "instance-state-name", "Values": ["pending", "running", "shutting-down", "stopping", "stopped"]},
            {"Name": "tag:Name", "Values": [f"*{PROJECT_TAG}*"]}
        ]
    )["Reservations"]
    for r in more_inst:
        for i in r["Instances"]:
            if i["InstanceId"] not in inst_ids:
                inst_ids.append(i["InstanceId"])

    if inst_ids:
        print(f"  -> Terminando {len(inst_ids)} instancias: {inst_ids}")
        ec2.terminate_instances(InstanceIds=inst_ids)
        print("     Esperando a que las instancias terminen...")
        waiter = ec2.get_waiter("instance_terminated")
        waiter.wait(InstanceIds=inst_ids)
        print("     Instancias terminadas.")
    else:
        print("  -> No se encontraron instancias EC2 activas.")
except Exception as e:
    print(f"Error con instancias EC2: {e}")

# 3. LAUNCH TEMPLATES
print("\n[3/9] Eliminando Launch Templates...")
try:
    lts = ec2.describe_launch_templates()["LaunchTemplates"]
    for lt in lts:
        name = lt["LaunchTemplateName"]
        if PROJECT_TAG in name.lower():
            print(f"  -> Eliminando Launch Template: {name}")
            ec2.delete_launch_template(LaunchTemplateId=lt["LaunchTemplateId"])
except Exception as e:
    print(f"Error eliminando Launch Templates: {e}")

# 4. LOAD BALANCERS Y TARGET GROUPS
print("\n[4/9] Eliminando Application Load Balancers y Target Groups...")
try:
    lbs = elbv2.describe_load_balancers()["LoadBalancers"]
    deleted_lb_arns = []
    for lb in lbs:
        name = lb["LoadBalancerName"]
        if PROJECT_TAG in name.lower():
            print(f"  -> Eliminando ALB: {name}")
            elbv2.delete_load_balancer(LoadBalancerArn=lb["LoadBalancerArn"])
            deleted_lb_arns.append(lb["LoadBalancerArn"])

    if deleted_lb_arns:
        print("     Esperando 15s para que los ALBs liberen sus interfaces de red...")
        time.sleep(15)

    tgs = elbv2.describe_target_groups()["TargetGroups"]
    for tg in tgs:
        name = tg["TargetGroupName"]
        if PROJECT_TAG in name.lower():
            print(f"  -> Eliminando Target Group: {name}")
            try:
                elbv2.delete_target_group(TargetGroupArn=tg["TargetGroupArn"])
            except Exception as e:
                print(f"     Aviso al eliminar TG {name}: {e}")
except Exception as e:
    print(f"Error eliminando ALBs/TGs: {e}")

# 5. NAT GATEWAYS Y ELASTIC IPS
print("\n[5/9] Eliminando NAT Gateways y liberando Elastic IPs...")
try:
    nat_gws = ec2.describe_nat_gateways(
        Filters=[{"Name": "state", "Values": ["pending", "available"]}]
    )["NatGateways"]
    deleting_nats = []
    for nat in nat_gws:
        tags = {t["Key"]: t["Value"] for t in nat.get("Tags", [])}
        name = tags.get("Name", "")
        if PROJECT_TAG in name.lower() or PROJECT_TAG in str(tags).lower():
            print(f"  -> Eliminando NAT Gateway: {nat['NatGatewayId']} ({name})")
            ec2.delete_nat_gateway(NatGatewayId=nat["NatGatewayId"])
            deleting_nats.append(nat["NatGatewayId"])

    if deleting_nats:
        print("     Esperando a que los NAT Gateways se eliminen completamente (puede tomar 1-2 mins)...")
        for _ in range(30):
            current = ec2.describe_nat_gateways(NatGatewayIds=deleting_nats)["NatGateways"]
            active = [n for n in current if n["State"] in ["pending", "available", "deleting"]]
            if not active or all(n["State"] == "deleted" for n in current):
                print("     NAT Gateways eliminados.")
                break
            time.sleep(10)

    # Liberar EIPs
    eips = ec2.describe_addresses()["Addresses"]
    for eip in eips:
        tags = {t["Key"]: t["Value"] for t in eip.get("Tags", [])}
        name = tags.get("Name", "")
        if PROJECT_TAG in name.lower() or PROJECT_TAG in str(tags).lower():
            print(f"  -> Liberando EIP: {eip.get('AllocationId')} ({eip.get('PublicIp')})")
            try:
                ec2.release_address(AllocationId=eip["AllocationId"])
            except Exception as e:
                print(f"     No se pudo liberar EIP {eip.get('AllocationId')}: {e}")
except Exception as e:
    print(f"Error con NAT Gateways/EIPs: {e}")

# 6. ENCONTRAR TODAS LAS VPCS DE CARIBEXPERIENCE
print("\n[6/9] Identificando VPCs de CaribeXperience...")
target_vpcs = []
try:
    vpcs = ec2.describe_vpcs()["Vpcs"]
    for vpc in vpcs:
        if vpc.get("IsDefault", False):
            continue
        tags = {t["Key"]: t["Value"] for t in vpc.get("Tags", [])}
        name = tags.get("Name", "")
        # Si tiene el tag o coincide con el CIDR 10.0.0.0/16 del proyecto
        if PROJECT_TAG in name.lower() or PROJECT_TAG in str(tags).lower() or vpc.get("CidrBlock") == "10.0.0.0/16":
            target_vpcs.append(vpc["VpcId"])
            print(f"  -> VPC objetivo encontrada: {vpc['VpcId']} (Name: {name}, CIDR: {vpc['CidrBlock']})")
except Exception as e:
    print(f"Error buscando VPCs: {e}")

# 7. ROUTE TABLES, INTERNET GATEWAYS, SUBNETS Y SECURITY GROUPS DENTRO DE LAS VPCS
for vpc_id in target_vpcs:
    print(f"\n[7/9] Limpiando dependencias dentro de VPC {vpc_id}...")

    # Desasociar y eliminar Security Groups personalizados
    try:
        sgs = ec2.describe_security_groups(Filters=[{"Name": "vpc-id", "Values": [vpc_id]}])["SecurityGroups"]
        for sg in sgs:
            if sg["GroupName"] != "default":
                # Limpiar reglas para evitar dependencias circulares
                try:
                    if sg.get("IpPermissions"):
                        ec2.revoke_security_group_ingress(GroupId=sg["GroupId"], IpPermissions=sg["IpPermissions"])
                    if sg.get("IpPermissionsEgress"):
                        ec2.revoke_security_group_egress(GroupId=sg["GroupId"], IpPermissions=sg["IpPermissionsEgress"])
                except Exception:
                    pass
        for sg in sgs:
            if sg["GroupName"] != "default":
                try:
                    print(f"  -> Eliminando SG: {sg['GroupName']} ({sg['GroupId']})")
                    ec2.delete_security_group(GroupId=sg["GroupId"])
                except Exception as e:
                    print(f"     Aviso SG {sg['GroupId']}: {e}")
    except Exception as e:
        print(f"Error limpiando SGs: {e}")

    # Tablas de ruteo
    try:
        rts = ec2.describe_route_tables(Filters=[{"Name": "vpc-id", "Values": [vpc_id]}])["RouteTables"]
        for rt in rts:
            # Ignorar tabla principal
            is_main = any(assoc.get("Main", False) for assoc in rt.get("Associations", []))
            if is_main:
                continue
            for assoc in rt.get("Associations", []):
                try:
                    ec2.disassociate_route_table(AssociationId=assoc["RouteTableAssociationId"])
                except Exception:
                    pass
            try:
                print(f"  -> Eliminando Route Table: {rt['RouteTableId']}")
                ec2.delete_route_table(RouteTableId=rt["RouteTableId"])
            except Exception as e:
                print(f"     Aviso RT {rt['RouteTableId']}: {e}")
    except Exception as e:
        print(f"Error limpiando Route Tables: {e}")

    # Subredes
    try:
        subnets = ec2.describe_subnets(Filters=[{"Name": "vpc-id", "Values": [vpc_id]}])["Subnets"]
        for s in subnets:
            print(f"  -> Eliminando Subnet: {s['SubnetId']}")
            try:
                ec2.delete_subnet(SubnetId=s["SubnetId"])
            except Exception as e:
                print(f"     Aviso Subnet {s['SubnetId']}: {e}")
    except Exception as e:
        print(f"Error limpiando subredes: {e}")

    # Internet Gateways
    try:
        igws = ec2.describe_internet_gateways(Filters=[{"Name": "attachment.vpc-id", "Values": [vpc_id]}])["InternetGateways"]
        for igw in igws:
            print(f"  -> Desasociando y eliminando IGW: {igw['InternetGatewayId']}")
            try:
                ec2.detach_internet_gateway(InternetGatewayId=igw["InternetGatewayId"], VpcId=vpc_id)
                ec2.delete_internet_gateway(InternetGatewayId=igw["InternetGatewayId"])
            except Exception as e:
                print(f"     Aviso IGW {igw['InternetGatewayId']}: {e}")
    except Exception as e:
        print(f"Error limpiando IGWs: {e}")

# 8. ELIMINAR VPCS
print("\n[8/9] Eliminando las VPCs objetivo...")
for vpc_id in target_vpcs:
    # Reintentar un par de veces por si alguna interfaz de red tarda en liberarse
    deleted = False
    for attempt in range(5):
        try:
            print(f"  -> Eliminando VPC: {vpc_id} (Intento {attempt+1})")
            ec2.delete_vpc(VpcId=vpc_id)
            print(f"     VPC {vpc_id} eliminada con éxito.")
            deleted = True
            break
        except Exception as e:
            print(f"     Esperando liberación de recursos en VPC {vpc_id} ({e})...")
            time.sleep(10)
    if not deleted:
        print(f"     [!] No se pudo eliminar VPC {vpc_id} automáticamente. Verifica en la consola.")

# 9. DYNAMODB Y S3 (LIMPIEZA DE ESTADO DE CARIBEXPERIENCE)
print("\n[9/9] Verificando recursos de estado (DynamoDB / S3)...")
try:
    dynamodb.delete_table(TableName="caribexperience-tf-locks")
    print("  -> Tabla DynamoDB 'caribexperience-tf-locks' eliminada.")
except Exception as e:
    pass

try:
    bucket_name = "caribexperience-tf-state-los-parrilleros"
    # Eliminar objetos y versiones
    try:
        versions = s3.list_object_versions(Bucket=bucket_name)
        to_delete = []
        for v in versions.get("Versions", []):
            to_delete.append({"Key": v["Key"], "VersionId": v["VersionId"]})
        for m in versions.get("DeleteMarkers", []):
            to_delete.append({"Key": m["Key"], "VersionId": m["VersionId"]})
        if to_delete:
            s3.delete_objects(Bucket=bucket_name, Delete={"Objects": to_delete})
        s3.delete_bucket(Bucket=bucket_name)
        print(f"  -> Bucket S3 '{bucket_name}' eliminado.")
    except Exception:
        pass
except Exception:
    pass

print("\n=== Limpieza completada exitosamente ===")
