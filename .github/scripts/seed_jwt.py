"""Initialize the JWT secret only if no current value exists (manual CD only)."""
import os
import secrets
import boto3

client = boto3.client('secretsmanager')
arn = os.environ['JWT_SECRET_ARN']
try:
    client.get_secret_value(SecretId=arn)
except client.exceptions.ResourceNotFoundException:
    client.put_secret_value(SecretId=arn, SecretString=secrets.token_urlsafe(48))
