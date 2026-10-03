# TP 6 — Ansible : configurer sans connexion manuelle (V8)

**Durée : 105 min** · théorie 12 · activité 15 · mini-TP 35 · fil rouge 33 · validation 10 · **VM** : `ci` (contrôleur) → `target` (géré) · **Format** : individuel
*(Couvre les « TP Ansible 1 à 7 » et 9 : problème, inventaire, premier playbook, serveur web, variables, templates, handlers, déploiement. Rôles et intégration Jenkins : challenge ici, puis TP07.)*

#### 1. Objectif pédagogique
Comprendre **pourquoi** la configuration manuelle des serveurs ne tient pas ; décrire l'état souhaité d'un serveur dans un playbook **idempotent** ; utiliser inventaire, variables, templates et handlers ; déployer l'application avec Ansible.

#### 2. Prérequis
TP04-05 (Docker, registre) ; savoir se connecter en SSH.

#### 3. Concept DevOps abordé
**Infrastructure as Code et idempotence.** On décrit *l'état voulu* (« nginx installé, port 8090, page présente »), Ansible calcule et applique seulement les écarts. Rejouer un playbook ne change rien s'il n'y a rien à changer.

#### 4. Problème réel à résoudre
« L'équipe doit préparer 20 serveurs identiques. Chaque technicien suit sa propre procédure, oublie une étape ou configure un port différent. Dix jours plus tard, personne ne sait pourquoi le serveur 14 se comporte autrement. »

#### 5. Activité pédagogique de découverte **[N1 Découverte]** (15 min) — « La procédure à la main »
Sur `target`, **à la main**, configurez un petit serveur web :
```bash
ssh vagrant@192.168.56.11
sudo apt-get update -y && sudo apt-get install -y nginx
sudo mkdir -p /var/www/demo
echo "<h1>Configuré à la main</h1>" | sudo tee /var/www/demo/index.html
printf 'server {\n listen 8090;\n root /var/www/demo;\n}\n' | sudo tee /etc/nginx/conf.d/demo.conf
sudo nginx -t && sudo systemctl reload nginx
exit
curl -s http://192.168.56.11:8090
```
Comptez les commandes. Puis, **entre voisins**, comparez : qui a oublié le `mkdir` ? le `reload` ? Quelle serait la procédure pour 20 serveurs ? Et pour **défaire** ?

Nettoyage : `ssh vagrant@192.168.56.11 "sudo rm -f /etc/nginx/conf.d/demo.conf && sudo rm -rf /var/www/demo && sudo systemctl reload nginx"`.

> 🎤 **Formateur** : notez au tableau « 6 commandes × 20 serveurs = 120 occasions d'erreur ». Premier aperçu d'Ansible : `ansible all -m ping`.

#### 6. Mini-TP **[N2 Application]** (35 min) — « Le serveur web par playbook »
- **Contexte** : refaire l'activité précédente avec Ansible.
- **Objectif** : inventaire, commandes ad hoc, playbook, variables, template, handler, idempotence.
- **Architecture** : `ci` (contrôleur) → SSH → `target` (Nginx sur le port 8090).
- **Fichiers** (`depot-tp/ansible/`) :

`inventory.ini` (fourni)
```ini
[webservers]
target ansible_host=192.168.56.11

[all:vars]
ansible_python_interpreter=/usr/bin/python3
```

`mini-tp/tp-nginx.yml`
```yaml
---
# Mini-TP Ansible : un serveur web configuré par variables, template et handler
- name: Serveur web de démonstration
  hosts: webservers
  become: true
  vars:
    web_port: 8090
    page_title: "Serveur configuré par Ansible"
    web_root: /var/www/demo
  tasks:
    - name: Installer nginx
      ansible.builtin.apt:
        name: nginx
        state: present
        update_cache: true
        cache_valid_time: 3600

    - name: Créer le répertoire du site
      ansible.builtin.file:
        path: "{{ web_root }}"
        state: directory
        mode: "0755"

    - name: Générer la page d'accueil
      ansible.builtin.template:
        src: index.html.j2
        dest: "{{ web_root }}/index.html"
        mode: "0644"

    - name: Configurer le site nginx
      ansible.builtin.template:
        src: demo.conf.j2
        dest: /etc/nginx/conf.d/demo.conf
        mode: "0644"
      notify: Recharger nginx

    - name: Nginx démarré et activé
      ansible.builtin.service:
        name: nginx
        state: started
        enabled: true

  handlers:
    - name: Recharger nginx
      ansible.builtin.service:
        name: nginx
        state: reloaded
```

`mini-tp/templates/index.html.j2`
```jinja2
<h1>{{ page_title }}</h1>
<p>Serveur : {{ ansible_hostname }} - port {{ web_port }}</p>
```

`mini-tp/templates/demo.conf.j2`
```jinja2
server {
    listen {{ web_port }};
    root {{ web_root }};
    index index.html;
}
```

**Étapes et commandes** (toujours depuis `~/devops-formation/ansible`, **jamais depuis `/vagrant`**)
```bash
cd ~/devops-formation/ansible
ansible all -m ping                                  # inventaire + connexion
ansible webservers -m setup -a "filter=ansible_distribution*"
ansible webservers -a "df -h /"                      # commande ad hoc

ansible-playbook mini-tp/tp-nginx.yml --check --diff # simulation
ansible-playbook mini-tp/tp-nginx.yml                # 1re exécution : changed
ansible-playbook mini-tp/tp-nginx.yml                # 2e exécution : changed=0 (idempotence)
curl -s http://192.168.56.11:8090

# Variables : changer le port et le titre sans toucher au playbook
ansible-playbook mini-tp/tp-nginx.yml -e web_port=8091 -e 'page_title="Titre modifié"'
curl -s http://192.168.56.11:8091

# Handler et réparation automatique
ssh vagrant@192.168.56.11 "sudo systemctl stop nginx"
ansible-playbook mini-tp/tp-nginx.yml                # nginx est redémarré
```

**Résultat attendu** : 2ᵉ exécution `changed=0` ; changement de port → seules les tâches `template` configuration et le handler **Recharger nginx** apparaissent en `changed` ; après arrêt manuel, le service est relancé par la tâche `service`.

**Erreurs fréquentes**
- `Permission denied (publickey)` : `ssh-copy-id vagrant@192.168.56.11` (mot de passe `vagrant`).
- Lancer depuis `/vagrant` : `ansible.cfg` ignoré (dossier world-writable).
- Indentation YAML ; oublier `become: true` (erreur de droits `apt`).
- Utiliser `command: apt install` au lieu du module `apt` (perd l'idempotence).

**Solution** : playbook fourni. Le point à faire dire : **le handler ne se déclenche que si le fichier de configuration a réellement changé**.

#### 7. Retour pédagogique
- **Appris** : inventaire (qui), module (quoi), variable (paramètre), template (fichier généré), handler (réaction à un changement), idempotence (rejouable sans danger).
- **Pourquoi en DevOps** : le playbook est la documentation exécutable ; il se relit en PR comme du code.
- **Problème résolu** : dérive de configuration (*configuration drift*), erreurs manuelles, reconstruction d'un serveur.
- **Limites** : Ansible décrit des étapes (pas un état garanti comme Kubernetes) ; un module mal choisi n'est pas idempotent ; les secrets exigent Ansible Vault.

#### 8. Retour au projet fil rouge **[N3 Intégration]** (33 min) — V8
**Objectif** : déployer le conteneur de l'application sur `target` **sans se connecter dessus**.
1. Lisez `ansible/deploy-app.yml` : `docker_image` (pull depuis le registre), `docker_container` (recréé, port 8080, `APP_VERSION`), attente de `/actuator/health`.
2. Vérifiez que `target` n'a plus de conteneur Compose (TP05, nettoyage) puis déployez :
   ```bash
   cd ~/devops-formation/ansible
   ansible-playbook deploy-app.yml -e image_tag=1.0
   curl -s http://192.168.56.11:8080/api/info
   ```
3. Déployez une autre version : poussez `1.1` (TP05, validation) puis `ansible-playbook deploy-app.yml -e image_tag=1.1` et vérifiez la version affichée.
4. **Discussion** : cette tâche est-elle idempotente ? (non : `recreate: true` force la recréation, choix volontaire pour garantir la version.)
5. **Option (site.yml)** : lisez `ansible/site.yml` (Java + Nginx + utilisateurs + service systemd) et repérez variables, handlers, tags ; exécutez `ansible-playbook site.yml --check --diff`.

**Résultat attendu** : `"version":"1.0"` puis `"version":"1.1"` sans qu'aucune commande n'ait été tapée sur `target`.

**Pourquoi V8 ?** Avant : on se connecte en SSH pour lancer `docker run`. Après : le déploiement est un fichier versionné, rejouable par Jenkins. Preuve : la version change après `ansible-playbook`.

#### 9. Validation
1. Que signifie « idempotent » ? Comment le prouver avec Ansible ?
2. Où déclare-t-on la liste des serveurs ? Les variables ?
3. Quand un handler s'exécute-t-il ?
4. Tâche : changez la version via `-e image_tag=` et prouvez-le avec `curl`.
5. Pourquoi ne pas lancer `ansible-playbook` depuis `/vagrant` ?

#### 10. Extension / challenge
- Transformez `tp-nginx.yml` en **rôle** : `ansible-galaxy init roles/nginx_demo`, déplacez tâches, templates, handlers et variables par défaut.
- Chiffrez une variable avec **Ansible Vault** (`ansible-vault encrypt_string`).
- Ajoutez un utilisateur opérateur dans la variable `operators` de `site.yml` et rejouez.
