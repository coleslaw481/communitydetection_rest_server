#!/usr/bin/env bash

# install needed packages (maven is needed to build REST service)
dnf install -y podman java-11-openjdk maven git

# create cdrunner user
adduser cdrunner

# create log directory
mkdir -p /var/log/communitydetection
chown cdrunner.cdrunner /var/log/communitydetection

# create tasks directory
mkdir -p /opt/communitydetection/tasks
ln -s /var/log/communitydetection /opt/communitydetection/logs
chown -R cdrunner.cdrunner /opt/communitydetection

# build REST jar
pushd /vagrant/
mvn install -Dmaven.test.skip=true
JAR_PATH=`/bin/ls target/communitydetection-rest-*with*dependencies.jar`
JAR_FILE=`basename $JAR_PATH`
cp $JAR_PATH /opt/communitydetection/.
popd
pushd /opt/communitydetection
ln -s /opt/communitydetection/${JAR_FILE} communitydetection-rest.jar
popd

# copy configuration from systemd/ 
cp /vagrant/systemdservice/communitydetection.conf /etc/.

# copy algorithms from systemd/ 
cp /vagrant/systemdservice/communitydetectionalgorithms.json /etc/.

# copy systemd service
cp /vagrant/systemdservice/communitydetection.service /lib/systemd/system

# enable communitydetection service
systemctl enable communitydetection

echo "Starting community detection service"
# start service
systemctl start communitydetection

systemctl status communitydetection

echo ""
echo "Community Detection REST Service server configured"
echo "Configuration file: /etc/communitydetection.conf"
echo "Log files: /var/log/communitydetection"
echo "Task directory: /opt/communitydetection/tasks"
echo "To stop service: systemctl stop communitydetection"
echo "To start service: systemctl start communitydetection"  
echo ""
echo "Visit http://localhost:8081/cd in your browser for swagger"
echo ""
echo "To update the service:"
echo " 1) Create new jar via mvn install from VM or host computer"
echo " 2) Connect to VM via vagrant ssh and become root (sudo -u root /bin/bash)"
echo " 3) Copy /vagrant/target/community*jar to /opt/communitydetection"
echo " 4) Update /opt/communitydetection/communitydetection-rest.jar symlink if needed"
echo " 5) Run systemctl restart communitydetection"
echo ""
echo "Have a nice day!!!"
echo ""

