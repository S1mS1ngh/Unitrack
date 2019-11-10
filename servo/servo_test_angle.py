from adafruit_servokit import ServoKit
import time

kit = ServoKit(channels = 16)
kit.servo[0].set_pulse_width_range(1000, 2000)
kit.servo[0].actuation_range = 120
kit.servo[0].angle = 0
while(1):

    time.sleep(.01)
    kit.servo[0].angle = kit.servo[0].angle + 1
    
    
kit.servo[0].angle = 0
