docker run -d \
  --name curtis-postgres \
  -e POSTGRES_DB=curtisdb \
  -e POSTGRES_USER=curtisuser \
  -e POSTGRES_PASSWORD=curtispass \
  -p 5432:5432 \
  -v pgdata:/var/lib/postgresql/data \
  docker.io/library/postgres:17
