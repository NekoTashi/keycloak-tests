docker_compose('docker-compose.yaml')

dc_resource('keycloak', labels=['auth'])
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
