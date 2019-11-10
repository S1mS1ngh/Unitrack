from board import SDA, SCL
import busio
import time

from adafruit_pca9685 import PCA9685

pwm_frequency = 60
pwm_min = 1400e-6
pwm_max = 1600e-6


def my_map(x, in_min, in_max, out_min, out_max):
    return int((x-in_min) * (out_max-out_min) / (in_max-in_min) + out_min)

def set_throttle(x):
    pca.channels[0].duty_cycle = my_map(x, -1, 1, pwm_min * pwm_frequency * 65535, pwm_max * pwm_frequency * 65535)

i2c_bus = busio.I2C(SCL, SDA)

pca = PCA9685(i2c_bus)

pca.frequency = pwm_frequency

duty_cycle = my_map(0, -1, 1, pwm_min * pwm_frequency * 65535, pwm_max * pwm_frequency * 65535)
print(duty_cycle)

pca.channels[0].duty_cycle = duty_cycle
time.sleep(1)

duty_cycle = my_map(0, -1, 1, pwm_min * pwm_frequency * 65535, pwm_max * pwm_frequency * 65535)
print(duty_cycle)

pca.channels[0].duty_cycle = duty_cycle
time.sleep(1)

duty_cycle = my_map(-1, -1, 1, pwm_min * pwm_frequency * 65535, pwm_max * pwm_frequency * 65535)
print(duty_cycle)

pca.channels[0].duty_cycle = duty_cycle
time.sleep(1)

duty_cycle = my_map(1, -1, 1, pwm_min * pwm_frequency * 65535, pwm_max * pwm_frequency * 65535)
print(duty_cycle)

pca.channels[0].duty_cycle = duty_cycle
time.sleep(1)

duty_cycle = my_map(0, -1, 1, pwm_min * pwm_frequency * 65535, pwm_max * pwm_frequency * 65535)
print(duty_cycle)

pca.channels[0].duty_cycle = duty_cycle
time.sleep(1)
