# -*- mode: ruby -*-
# vi: set ft=ruby :
# =============================================================================
#  FORMATION DEVOPS (2 jours) - Infrastructure d'exécution
# -----------------------------------------------------------------------------
#  5 VM Ubuntu 22.04 sur un réseau privé 192.168.56.0/24 :
#
#   ci       192.168.56.10  Jenkins, JDK 21, Maven, Git, Docker, registre privé,
#                           Ansible, Chrome (Selenium)            (TP1 TP2 TP3 TP4 TP8)
#   target   192.168.56.11  Serveur cible : Docker + SSH (Nginx/Java via Ansible) (TP2 TP3 TP4 TP5 TP8)
#   monitor  192.168.56.12  Prometheus, Alertmanager, Grafana (Docker Compose)    (TP6)
#   elk      192.168.56.13  Elasticsearch, Logstash, Kibana (Docker Compose)      (TP5)
#   k8s      192.168.56.14  Kubernetes mono-noeud (k3s)                          (TP7)
#
#  Usage :
#    vagrant up                      # démarre ci + target (jour 1)
#    vagrant up monitor elk k8s      # jour 2
#    LAB_ALL=1 vagrant up            # tout démarrer (~16 Go de RAM requis)
#    vagrant ssh ci                  # se connecter
#    vagrant provision <vm>          # rejouer le provisionnement
#    vagrant destroy -f              # tout supprimer
#
#  RAM ajustable : CI_RAM, TARGET_RAM, MONITOR_RAM, ELK_RAM, K8S_RAM (en Mo)
# =============================================================================

BOX = "ubuntu/jammy64"

# ---------------------------------------------------------------------------
# Script commun à toutes les VM
# ---------------------------------------------------------------------------
COMMON = <<~'SHELL'
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive

grep -q "# devops-lab" /etc/hosts || cat >> /etc/hosts <<'EOF'
# devops-lab
192.168.56.10 ci jenkins registry
192.168.56.11 target
192.168.56.12 monitor prometheus grafana
192.168.56.13 elk kibana
192.168.56.14 k8s
EOF

timedatectl set-timezone Africa/Tunis || true
apt-get update -y
apt-get install -y curl wget git vim unzip jq net-tools ca-certificates gnupg rsync htop prometheus-node-exporter
systemctl enable --now prometheus-node-exporter
echo ">>> [common] OK ($(hostname))"
SHELL

# ---------------------------------------------------------------------------
# Docker Engine + Compose (ci, target, monitor, elk)
# ---------------------------------------------------------------------------
DOCKER = <<~'SHELL'
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive
if ! command -v docker >/dev/null 2>&1; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" > /etc/apt/sources.list.d/docker.list
  apt-get update -y
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "insecure-registries": ["192.168.56.10:5000"],
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" }
}
EOF
systemctl enable docker
systemctl restart docker
usermod -aG docker vagrant
echo ">>> [docker] OK : $(docker --version)"
SHELL

# ---------------------------------------------------------------------------
# VM "ci" : Jenkins + outils de build + registre + Ansible + Chrome
# ---------------------------------------------------------------------------
CI = <<~'SHELL'
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive

echo ">>> [ci] JDK 21, Maven, Python"
apt-get install -y openjdk-21-jdk maven python3-pip sshpass

echo ">>> [ci] Ansible + collection community.docker"
pip3 install --quiet "ansible-core>=2.16,<2.18"
ansible-galaxy collection install "community.docker:>=3.10.0,<4.0.0" -p /usr/share/ansible/collections

echo ">>> [ci] Google Chrome (tests Selenium headless)"
if ! command -v google-chrome >/dev/null 2>&1; then
  wget -q -O /tmp/chrome.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
  apt-get install -y /tmp/chrome.deb
fi

echo ">>> [ci] Registre Docker privé sur 192.168.56.10:5000"
if ! docker ps -a --format '{{.Names}}' | grep -qx registry; then
  docker run -d --restart=always --name registry -p 5000:5000 -v /opt/registry:/var/lib/registry registry:2
fi

echo ">>> [ci] Configuration Git + clé SSH partagée"
sudo -u vagrant git config --global user.name "Stagiaire DevOps"
sudo -u vagrant git config --global user.email "stagiaire@formation.local"
sudo -u vagrant git config --global init.defaultBranch main
sudo -u vagrant bash -c '[ -f ~/.ssh/id_ed25519 ] || ssh-keygen -q -t ed25519 -N "" -f ~/.ssh/id_ed25519'
cat > /home/vagrant/.ssh/config <<'EOF'
Host 192.168.56.* target k8s
  StrictHostKeyChecking no
  UserKnownHostsFile /dev/null
  LogLevel ERROR
EOF
chown vagrant:vagrant /home/vagrant/.ssh/config
chmod 600 /home/vagrant/.ssh/config
mkdir -p /vagrant/.shared
cp /home/vagrant/.ssh/id_ed25519.pub /vagrant/.shared/ci_key.pub

echo ">>> [ci] Copie du dépôt de TP dans ~/devops-formation"
if [ ! -d /home/vagrant/devops-formation ]; then
  rsync -a --exclude .vagrant --exclude .shared /vagrant/ /home/vagrant/devops-formation/
  chown -R vagrant:vagrant /home/vagrant/devops-formation
fi

echo ">>> [ci] Installation de Jenkins"
if ! dpkg -s jenkins >/dev/null 2>&1; then
  curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key -o /usr/share/keyrings/jenkins-keyring.asc
  echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/" > /etc/apt/sources.list.d/jenkins.list
  apt-get update -y
  printf '#!/bin/sh\nexit 101\n' > /usr/sbin/policy-rc.d
  chmod +x /usr/sbin/policy-rc.d
  apt-get install -y jenkins
  rm -f /usr/sbin/policy-rc.d
fi
systemctl stop jenkins || true
usermod -aG docker jenkins

mkdir -p /etc/systemd/system/jenkins.service.d
cat > /etc/systemd/system/jenkins.service.d/override.conf <<'EOF'
[Service]
Environment="JAVA_OPTS=-Djava.awt.headless=true -Djenkins.install.runSetupWizard=false -Xmx1g"
EOF

mkdir -p /var/lib/jenkins/init.groovy.d /var/lib/jenkins/plugins
cat > /var/lib/jenkins/init.groovy.d/01-admin.groovy <<'EOF'
import jenkins.model.*
import hudson.security.*
def j = Jenkins.get()
def realm = new HudsonPrivateSecurityRealm(false)
try { realm.createAccount("admin", "admin") } catch (Exception e) { /* existe déjà */ }
j.setSecurityRealm(realm)
def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
strategy.setAllowAnonymousRead(false)
j.setAuthorizationStrategy(strategy)
JenkinsLocationConfiguration.get().setUrl("http://192.168.56.10:8080/")
j.save()
EOF

# Clé SSH + known_hosts permissifs pour l'utilisateur jenkins (déploiement vers target/k8s)
install -d -o jenkins -g jenkins -m 700 /var/lib/jenkins/.ssh
install -o jenkins -g jenkins -m 600 /home/vagrant/.ssh/id_ed25519 /var/lib/jenkins/.ssh/id_ed25519
install -o jenkins -g jenkins -m 600 /home/vagrant/.ssh/config /var/lib/jenkins/.ssh/config

if [ ! -f /var/lib/jenkins/plugins/workflow-aggregator.jpi ]; then
  echo ">>> [ci] Plugins Jenkins (2-4 minutes)"
  JPM_URL=$(curl -fsSL https://api.github.com/repos/jenkinsci/plugin-installation-manager-tool/releases/latest | jq -r '.assets[] | select(.name|endswith(".jar")) | .browser_download_url' | head -n1)
  curl -fsSL -o /opt/jenkins-plugin-manager.jar "$JPM_URL"
  java -jar /opt/jenkins-plugin-manager.jar --war /usr/share/java/jenkins.war \
    --plugin-download-directory /var/lib/jenkins/plugins \
    --plugins git github workflow-aggregator pipeline-stage-view junit docker-workflow \
              credentials-binding ssh-agent timestamper ws-cleanup ansicolor prometheus
fi
chown -R jenkins:jenkins /var/lib/jenkins

systemctl daemon-reload
systemctl enable --now jenkins
echo ">>> [ci] Attente du démarrage de Jenkins..."
for i in $(seq 1 90); do
  code=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/login || true)
  [ "$code" = "200" ] && break
  sleep 5
done

echo ">>> [ci] Pré-chargement du cache Maven (vagrant + jenkins)"
rm -rf /tmp/warm && mkdir -p /tmp/warm/app /tmp/warm/sel
cp /vagrant/app/pom.xml /tmp/warm/app/ && cp /vagrant/selenium-tests/pom.xml /tmp/warm/sel/
chmod -R a+rX /tmp/warm
for u in vagrant jenkins; do
  sudo -u "$u" -H bash -c 'cd /tmp && mvn -B -q -f /tmp/warm/app/pom.xml dependency:go-offline; mvn -B -q -f /tmp/warm/sel/pom.xml dependency:go-offline' || true
done

echo "=============================================================="
echo " Jenkins   : http://192.168.56.10:8080   (admin / admin)"
echo " Registre  : http://192.168.56.10:5000/v2/_catalog"
echo "=============================================================="
SHELL

# ---------------------------------------------------------------------------
# VM "target" : serveur cible (Nginx et Java sont installés par Ansible au TP2)
# ---------------------------------------------------------------------------
TARGET = <<~'SHELL'
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive
apt-get install -y python3 python3-apt python3-docker

# Authentification SSH par mot de passe autorisée (pour montrer ssh-copy-id)
echo 'vagrant:vagrant' | chpasswd
echo "PasswordAuthentication yes" > /etc/ssh/sshd_config.d/01-lab.conf
systemctl reload ssh

# Clé publique de la VM ci (générée par son provisionnement)
install -d -m 700 -o vagrant -g vagrant /home/vagrant/.ssh
if [ -f /vagrant/.shared/ci_key.pub ]; then
  grep -qF "$(cat /vagrant/.shared/ci_key.pub)" /home/vagrant/.ssh/authorized_keys || cat /vagrant/.shared/ci_key.pub >> /home/vagrant/.ssh/authorized_keys
  echo ">>> [target] clé SSH de ci autorisée"
else
  echo "!!! [target] /vagrant/.shared/ci_key.pub absent : démarrez 'ci' d'abord puis 'vagrant provision target'"
fi
echo "=============================================================="
echo " Cible : 192.168.56.11  (app Docker :8080, Nginx :80)"
echo "=============================================================="
SHELL

# ---------------------------------------------------------------------------
# VM "monitor" : Prometheus + Alertmanager + Grafana
# ---------------------------------------------------------------------------
MONITOR = <<~'SHELL'
set -euo pipefail
cd /vagrant/monitoring
docker compose pull
docker compose up -d
echo "=============================================================="
echo " Prometheus   : http://192.168.56.12:9090"
echo " Alertmanager : http://192.168.56.12:9093"
echo " Grafana      : http://192.168.56.12:3000   (admin / admin)"
echo "=============================================================="
SHELL

# ---------------------------------------------------------------------------
# VM "elk" : Elasticsearch + Logstash + Kibana
# ---------------------------------------------------------------------------
ELK = <<~'SHELL'
set -euo pipefail
sysctl -w vm.max_map_count=262144
echo "vm.max_map_count=262144" > /etc/sysctl.d/99-elasticsearch.conf
cd /vagrant/elk
docker compose pull
docker compose up -d
echo "=============================================================="
echo " Kibana        : http://192.168.56.13:5601  (patientez 2-3 min)"
echo " Elasticsearch : http://192.168.56.13:9200"
echo " Logstash      : 192.168.56.13:5044 (entrée Beats)"
echo "=============================================================="
SHELL

# ---------------------------------------------------------------------------
# VM "k8s" : Kubernetes mono-noeud avec k3s
# ---------------------------------------------------------------------------
K8S = <<~'SHELL'
set -euo pipefail
IFACE=$(ip -o -4 addr show | awk '$4 ~ /^192\.168\.56\.14\// {print $2}')

# Le registre privé de la VM ci est en HTTP : on l'autorise pour containerd
mkdir -p /etc/rancher/k3s
cat > /etc/rancher/k3s/registries.yaml <<'EOF'
mirrors:
  "192.168.56.10:5000":
    endpoint:
      - "http://192.168.56.10:5000"
EOF

if ! command -v k3s >/dev/null 2>&1; then
  curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="server --write-kubeconfig-mode 644 --disable traefik --node-ip 192.168.56.14 --advertise-address 192.168.56.14 --tls-san 192.168.56.14 --flannel-iface ${IFACE}" sh -
fi

export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
until kubectl get nodes 2>/dev/null | grep -q ' Ready'; do sleep 5; done

install -d -o vagrant -g vagrant /home/vagrant/.kube
cp /etc/rancher/k3s/k3s.yaml /home/vagrant/.kube/config
chown vagrant:vagrant /home/vagrant/.kube/config
grep -q "alias k=" /home/vagrant/.bashrc || echo "alias k=kubectl" >> /home/vagrant/.bashrc

install -d -m 700 -o vagrant -g vagrant /home/vagrant/.ssh
if [ -f /vagrant/.shared/ci_key.pub ]; then
  grep -qF "$(cat /vagrant/.shared/ci_key.pub)" /home/vagrant/.ssh/authorized_keys || cat /vagrant/.shared/ci_key.pub >> /home/vagrant/.ssh/authorized_keys
fi
echo "=============================================================="
echo " Kubernetes : vagrant ssh k8s  puis  kubectl get nodes"
echo " NodePort de l'appli : http://192.168.56.14:30080"
echo "=============================================================="
SHELL

# ---------------------------------------------------------------------------
# Définition des machines
# ---------------------------------------------------------------------------
MACHINES = [
  { name: "ci",      ip: "192.168.56.10", ram: ENV.fetch("CI_RAM", "4096"),      cpus: 2, docker: true,  script: CI,      auto: true  },
  { name: "target",  ip: "192.168.56.11", ram: ENV.fetch("TARGET_RAM", "2048"),  cpus: 1, docker: true,  script: TARGET,  auto: true  },
  { name: "monitor", ip: "192.168.56.12", ram: ENV.fetch("MONITOR_RAM", "2048"), cpus: 1, docker: true,  script: MONITOR, auto: false },
  { name: "elk",     ip: "192.168.56.13", ram: ENV.fetch("ELK_RAM", "4608"),     cpus: 2, docker: true,  script: ELK,     auto: false },
  { name: "k8s",     ip: "192.168.56.14", ram: ENV.fetch("K8S_RAM", "3072"),     cpus: 2, docker: false, script: K8S,     auto: false },
]

Vagrant.configure("2") do |config|
  config.vm.box = BOX
  config.vm.box_check_update = false
  config.vm.boot_timeout = 600

  MACHINES.each do |m|
    config.vm.define m[:name], autostart: (m[:auto] || ENV["LAB_ALL"] == "1") do |node|
      node.vm.hostname = m[:name]
      node.vm.network "private_network", ip: m[:ip]

      node.vm.provider "virtualbox" do |vb|
        vb.name   = "devops-#{m[:name]}"
        vb.memory = m[:ram]
        vb.cpus   = m[:cpus]
        vb.linked_clone = true
        vb.customize ["modifyvm", :id, "--natdnshostresolver1", "on"]
      end

      node.vm.provision "common", type: "shell", inline: COMMON
      node.vm.provision "docker", type: "shell", inline: DOCKER if m[:docker]
      node.vm.provision m[:name], type: "shell", inline: m[:script]
    end
  end
end
