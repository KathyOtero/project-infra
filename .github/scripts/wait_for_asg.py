"""Wait for healthy capacity and for the latest instance refresh to finish."""
import os
import time
import boto3

client = boto3.client('autoscaling')
elb = boto3.client('elbv2')
name = os.environ['ASG_NAME']
deadline = time.monotonic() + 1800
while time.monotonic() < deadline:
    group = client.describe_auto_scaling_groups(AutoScalingGroupNames=[name])['AutoScalingGroups'][0]
    refreshes = client.describe_instance_refreshes(AutoScalingGroupName=name, MaxRecords=1)['InstanceRefreshes']
    status = refreshes[0]['Status'] if refreshes else 'Successful'
    if status in ('Failed', 'Cancelled', 'RollbackFailed', 'RollbackSuccessful'):
        raise RuntimeError(f'Instance refresh did not succeed: {status}')
    healthy = [i for i in group['Instances'] if i['HealthStatus'] == 'Healthy' and i['LifecycleState'] == 'InService']
    targets = elb.describe_target_health(TargetGroupArn=os.environ['TARGET_GROUP_ARN'])['TargetHealthDescriptions']
    healthy_ids = {target['Target']['Id'] for target in targets if target['TargetHealth']['State'] == 'healthy'}
    healthy = [instance for instance in healthy if instance['InstanceId'] in healthy_ids]
    if status == 'Successful' and len(healthy) >= group['DesiredCapacity']:
        break
    time.sleep(20)
else:
    raise TimeoutError('Auto Scaling deployment did not become healthy in 30 minutes')
