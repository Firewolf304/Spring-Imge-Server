Simple Spring server
Installing (fedora 40):
```
cd ~
sudo dnf update
sudo dnf install dnf5
sudo dnf5 install git java-17-openjdk maven wget
wget https://dlcdn.apache.org/tomcat/tomcat-9/v9.0.90/bin/apache-tomcat-9.0.90.tar.gz
```
change java version:
 ```sudo alternatives --config java```
```
Use this guide for continue: https://www.atlantic.net/dedicated-server-hosting/how-to-install-tomcat-on-fedora/
```
Installing docker:
```
docker image pull tomcat:9.0
sudo docker container create --publish 8082:8080 --name tomcat-server tomcat:9.0
docker start my-tomcat-container
git clone https://github.com/Firewolf304/Spring-Imge-Server.git
cd Spring-Imge-Server
mvn clean package
cd target
sudo docker cp testSpringAuth-1.0-SNAPSHOT.war tomcat-server:/usr/local/tomcat/webapps/

```