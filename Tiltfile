docker_compose('docker-compose.yaml')

dc_resource('keycloak', labels=['auth'], trigger_mode=TRIGGER_MODE_MANUAL)
dc_resource('postgres', labels=['database'])

local_resource(
  'react-app',
  cmd='cd apps/react && npm install',
  serve_cmd='npm run dev',
  serve_dir='apps/react',
  deps=['apps/react/src'],
  resource_deps=['keycloak'],
  labels=['frontend'],
)

local_resource(
  'nextjs-app',
  cmd='cd apps/next && npm install',
  serve_cmd='npm run dev',
  serve_dir='apps/next',
  deps=['apps/next/app', 'apps/next/lib'],
  resource_deps=['keycloak'],
  labels=['frontend'],
)
