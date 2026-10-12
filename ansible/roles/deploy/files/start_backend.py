"""Fetch runtime secrets without writing them to inventory or Terraform state."""
import base64
import json
import os
from pathlib import Path
import subprocess
import tempfile

import boto3


def main():
    config = json.loads(Path('/opt/caribexperience/config.json').read_text())
    region = config['aws_region']
    secrets = boto3.client('secretsmanager', region_name=region)
    database = json.loads(secrets.get_secret_value(SecretId=config['db_secret_arn'])['SecretString'])
    jwt = secrets.get_secret_value(SecretId=config['jwt_secret_arn'])['SecretString']
    auth = boto3.client('ecr', region_name=region).get_authorization_token()['authorizationData'][0]
    username, password = base64.b64decode(auth['authorizationToken']).decode().split(':', 1)
    with tempfile.TemporaryDirectory(dir='/run') as docker_config:
        docker_env = dict(os.environ, DOCKER_CONFIG=docker_config)
        subprocess.run(['docker', 'login', '--username', username, '--password-stdin', auth['proxyEndpoint']], input=password, text=True, check=True, env=docker_env, stdout=subprocess.DEVNULL)
        subprocess.run(['docker', 'pull', config['backend_image']], check=True, env=docker_env)
    subprocess.run(['docker', 'rm', '-f', 'caribexperience-backend'], check=False, stdout=subprocess.DEVNULL)
    values = {
        'DB_URL': f"jdbc:mysql://{config['db_endpoint']}:3306/caribexperience?sslMode=REQUIRED",
        'DB_USERNAME': database['username'], 'DB_PASSWORD': database['password'],
        'JWT_SECRET': jwt, 'CORS_ALLOWED_ORIGINS': config['cors_origin'],
    }
    if any('\n' in value or '\r' in value for value in values.values()):
        raise ValueError('Multiline environment values are unsupported')
    with tempfile.NamedTemporaryFile(mode='w', dir='/run', prefix='caribe-', delete=False) as env_file:
        env_path = env_file.name
        os.chmod(env_path, 0o600)
        for key, value in values.items():
            env_file.write(f'{key}={value}\n')
    try:
        subprocess.run(['docker', 'create', '--name', 'caribexperience-backend', '--env-file', env_path,
                        '-p', '127.0.0.1:8080:8080', '--log-driver', 'awslogs',
                        '--log-opt', f'awslogs-region={region}', '--log-opt', f"awslogs-group={config['log_group']}",
                        config['backend_image']], check=True, stdout=subprocess.DEVNULL)
    finally:
        Path(env_path).unlink(missing_ok=True)
    subprocess.run(['docker', 'start', '-a', 'caribexperience-backend'], check=True)


if __name__ == '__main__':
    main()
