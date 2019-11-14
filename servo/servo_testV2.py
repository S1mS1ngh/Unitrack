from adafruit_servokit import ServoKit
kit = ServoKit(channels = 16)
kit.continuous_servo[0].set_pulse_width_range(700, 2300)
kit.continuous_servo[0].throttle = .1