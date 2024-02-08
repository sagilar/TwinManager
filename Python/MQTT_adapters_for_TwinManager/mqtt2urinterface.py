# Author: Santiago Gil
import paho.mqtt.client as mqtt
import urinterface.robot_connection as urconn # See: https://gitlab.au.dk/clagms/urinterface
import time
import numpy as np
import csv
from threading import Thread

# Attributes - initialized from schema
attributes = {}
operations = ["movel","movej","movep"]

## MQTT client
mqtt_client = mqtt.Client()
mqtt_broker_ip = "localhost"
mqtt_broker_port = 1883
base_topic = "ur5e/"

## Define the parameters for connecting with the real robot

ur5e_ip = "localhost"
ur5e_dashboard_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
ur5e_controller_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
ur5e = urconn.RobotConnection(ur5e_ip,controller_socket=ur5e_controller_socket,dashboard_socket=ur5e_dashboard_socket) # Establish dashboard connection (port 29999) and controller connection (port 30002)
f_name = "ur5e_actual.csv"
ur5e.start_recording(filename=f_name, overwrite=True, frequency=50) # start recording and place the recorded data in csv file

def forward_command(command):
    if "movel" in command:
        index = command.index("(")
        values_str = command[index+1:-2]
        values_str_split = values_str.split(",")
        str_array = np.array(values_str_split)
        q = str_array.astype(np.float)
        ur5e.movel(q=q)
    elif "movej" in command:
        index = command.index("(")
        values_str = command[index+1:-2]
        values_str_split = values_str.split(",")
        str_array = np.array(values_str_split)
        q = str_array.astype(np.float)
        q = np.radians(q)
        ur5e.movej(q=q)
    elif "movep" in command:
        index = command.index("(")
        values_str = command[index+1:-2]
        values_str_split = values_str.split(",")
        str_array = np.array(values_str_split)
        q = str_array.astype(np.float)
        ur5e.movep(p=q)

def publish_topics(client,topic_prefix,filename):
    while(True):
        topic_list = []
        with open(filename) as csv_file:
            csv_reader = csv.DictReader(csv_file,delimiter=" ")
            dict_from_csv = dict(list(csv_reader)[0])
            topic_list = list(dict_from_csv.keys())
        with open(filename) as csv_file:
            final_line = csv_file.readlines()[-1].split(" ")
        for i in range(len(topic_list)):
            client.publish(topic_prefix + topic_list[i],final_line[i])
        time.sleep(0.5)

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

publishing_thread = Thread(target=publish_topics,args=(mqtt_client,base_topic + "/",f_name,))
publishing_thread.daemon = True
publishing_thread.start()

mqtt_client.loop_forever()

ur5e.stop_recording()
