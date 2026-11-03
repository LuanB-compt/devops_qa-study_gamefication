FROM prom/prometheus:latest

# Config embutida na imagem: o Jenkins roda em container e usa o Docker do host,
# entao bind mounts relativos ao workspace nao existem no host.
COPY prometheus.yml /etc/prometheus/prometheus.yml
