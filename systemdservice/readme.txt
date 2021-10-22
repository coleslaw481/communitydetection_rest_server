The instructions in this readme provide steps
to install Community Detection REST service as a service managed by systemd.
These instructions use the files in this directory and require
a Rocky Linux 8 box with superuser access.

# Requirements

* podman (dnf install podman)
* cdrunner user added (useradd cdrunner)
* cdrunner user added to docker group (usermod -a -G docker cdrunner)
* java 11 installed and in path
* if doing apache run this: /usr/sbin/setsebool -P httpd_can_network_connect=1
  to enable the port redirection

1) Create needed directories as super user

mkdir -p /var/log/communitydetection
chown cdrunner.cdrunner /var/log/communitydetection
mkdir -p /opt/communitydetection/tasks
ln -s /var/log/communitydetection /opt/communitydetection/logs
chown -R cdrunner.cdrunner /opt/communitydetection

2) Create conf file

Copy communitydetection.conf to /etc

NOTE: If this will be a diffusion service, uncomment communitydetection.diffusion.algorithm property

3) Create algorithms file

Copy communitydetectionalgorithms.json to /etc

NOTE: If this will be a diffusion service, copy diffusionalgorithm.json to /etc/communitydetectionalgorithms.json

4) Copy jar

Build community detection jar with dependencies and put
in /opt/communitydetection directory. Also
create a hardlink to the specific jar named communitydetection-rest.jar
(symlink won't work for systemd)

5) Create systemd file

Copy communitydetection.service to /lib/systemd/system
cd /lib/systemd/system

6) Register script with systemd

systemctl daemon-reload
cd /lib/systemd/system
systemctl enable communitydetection
systemctl start communitydetection

7) Verify its running

ps -elf | grep communitydetection

# output
4 S cdrunner 18929     1 19  80   0 - 9280026 futex_ 11:57 ?      00:00:01 /bin/java -jar /opt/communitydetection/communitydetection-rest.jar --mode runserver --conf /etc/communitydetection.conf
