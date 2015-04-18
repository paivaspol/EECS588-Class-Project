for ((i = 0; i < 20; ++i))
do
	echo "serviceName=vaspol" > user${i}.properties
	echo "username=user${i}" >> user${i}.properties
	echo "password=password" >> user${i}.properties
	echo "host=35.2.115.91" >> user${i}.properties
	echo "port=5222" >> user${i}.properties
	echo "multiUserChatService=conference.vaspol" >> user${i}.properties
done