"""Runtime contract tests: secret handling and immutable image invocation."""
import importlib.util
import json
from pathlib import Path
import unittest
from unittest.mock import MagicMock, patch

SCRIPT = Path(__file__).resolve().parents[1] / 'ansible/roles/deploy/files/start_backend.py'
spec = importlib.util.spec_from_file_location('runtime', SCRIPT)
runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runtime)


class RuntimeTest(unittest.TestCase):
    def test_secret_fetch_and_cleanup_before_start(self):
        config = dict(aws_region='us-east-1', db_endpoint='db.internal', db_secret_arn='db',
                      jwt_secret_arn='jwt', backend_image='registry/backend:commit',
                      cors_origin='https://example.org', log_group='/caribe/backend')
        sm = MagicMock()
        sm.get_secret_value.side_effect = [dict(SecretString=json.dumps(dict(username='user', password='pass'))), dict(SecretString='jwt-value')]
        ecr = MagicMock()
        ecr.get_authorization_token.return_value = dict(authorizationData=[dict(authorizationToken='QVdTOnBhc3M=', proxyEndpoint='registry')])
        calls = []

        def run(args, **kwargs):
            calls.append(args)
            self.assertNotIn('pass', args)
            self.assertNotIn('jwt-value', args)
            if args[1] == 'create':
                env_path = Path(args[args.index('--env-file') + 1])
                self.assertIn('sslMode=REQUIRED', env_path.read_text())
                self.assertEqual(env_path.stat().st_mode & 0o777, 0o600)
            if args[1] == 'start':
                create = next(c for c in calls if c[1] == 'create')
                self.assertFalse(Path(create[create.index('--env-file') + 1]).exists())

        with patch.object(runtime.Path, 'read_text', autospec=True) as read, patch.object(runtime.boto3, 'client', side_effect=[sm, ecr]), patch.object(runtime.subprocess, 'run', side_effect=run):
            original_open = Path.open
            def read_file(path, *args, **kwargs):
                if str(path) == '/opt/caribexperience/config.json':
                    return json.dumps(config)
                with original_open(path) as source:
                    return source.read()
            read.side_effect = read_file
            runtime.main()
        self.assertIn(['docker', 'start', '-a', 'caribexperience-backend'], calls)


if __name__ == '__main__':
    unittest.main()
