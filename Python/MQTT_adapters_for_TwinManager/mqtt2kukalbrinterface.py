# Author: Santiago Gil
import paho.mqtt.client as mqtt
import kukalbrinterface
import time
import numpy as np
import csv
from threading import Thread

# Attributes - initialized from schema
attributes = {}
operations = ["moveptprad","moveptpcart","movelrel","movelin","movecirc"]

## MQTT client
mqtt_client = mqtt.Client()
mqtt_broker_ip = "localhost"
mqtt_broker_port = 1883
base_topic = "kuka/"

kuka_ip = "localhost"
kuka_robot = kukalbrinterface.RobotConnection(kuka_ip,enabled_mqtt=True,addr_mqtt=mqtt_broker_ip,port_mqtt=mqtt_broker_port,mqtt_topic=base_topic)#,mqtt_username="",mqtt_password="")


def forward_command(command):
    if "moveptprad" in command:
        index = command.index("(")
        values_str = command[index+1:-2]
        values_str_split = values_str.split(",")
        str_array = np.array(values_str_split)
        q = str_array.astype(np.float)
        kuka_robot.move_ptp_rad(q=q)
    if "moveptpcart" in command:
        index = command.index("(")
        values_str = command[index+1:-2]
        values_str_split = values_str.split(",")
        str_array = np.array(values_str_split)
        q = str_array.astype(np.float)
        kuka_robot.move_ptp_cart(q=q)
    elif "movelrel" in command:
        index = command.index("(")
        values_str = command[index+1:-2]
        values_str_split = values_str.split(",")
        str_array = np.array(values_str_split)
        q = str_array.astype(np.float)
        kuka_robot.move_l_rel(q=q)
    elif "movelin" in command:
        index = command.index("(")
        values_str = command[index+1:-2]
        values_str_split = values_str.split(",")
        str_array = np.array(values_str_split)
        q = str_array.astype(np.float)
        kuka_robot.move_l(q=q)
    elif "movecirc" in command:
        index = command.index("(")
        values_str = command[index+1:-2]
        values_str_split = values_str.split(",")
        str_array = np.array(values_str_split)
        q = str_array.astype(np.float)
        kuka_robot.movep(q=q)

def on_connect(client, userdata, flags, rc):
    print("Connected with result code "+str(rc))
    # Subscribing in on_connect() means that if we lose the connection and
    # reconnect then subscriptions will be renewed.
    mqtt_client.subscribe(base_topic + "#")

# The callback for when a PUBLISH message is received from the server.
def on_message(client, userdata, msg):
    print(msg.topic+" "+str(msg.payload))
    payload = str(msg.payload).replace("'","").replace("b","")

    if (base_topic in msg.topic):
        extra_topic = msg.topic.split("/")[1]
        if extra_topic in operations:
            forward_command(extra_topic + "(" + payload + ")")
        else:
            attributes[extra_topic] = payload


mqtt_client.on_connect = on_connect
mqtt_client.on_message = on_message
mqtt_client.connect(mqtt_broker_ip, mqtt_broker_port, 60)

mqtt_client.loop_forever()

kuka_robot.close()
