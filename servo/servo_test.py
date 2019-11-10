from adafruit_servokit import ServoKit
import time

kit = ServoKit(channels = 16)
kit.continuous_servo[0].set_pulse_width_range(700, 2300)
while(1):
    kit.continuous_servo[0].throttle = 0
    time.sleep(1)
    kit.continuous_servo[0].throttle = -1
    time.sleep(1)
    kit.continuous_servo[0].throttle = 0

#for x in range(3):
#    
#    kit.continuous_servo[0].throttle = .5
#    time.sleep(1)
#    kit.continuous_servo[0].throttle = -1
#    time.sleep(1)
#    
#kit.continuous_servo[0].throttle = 0
