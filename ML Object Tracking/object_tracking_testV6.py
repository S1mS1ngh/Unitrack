# import the necessary packages
from AWSIoTPythonSDK.MQTTLib import AWSIoTMQTTClient
from imutils.video import VideoStream
from imutils.video import FPS
import logging
import json
import argparse
import imutils
import time
import cv2
import picamera
import time
from adafruit_servokit import ServoKit
import requests
import sys
import dropbox
from dropbox.files import WriteMode
from dropbox.exceptions import ApiError, AuthError

topic = "demo-topic"

myMQTTClient = AWSIoTMQTTClient("Unitrack")

myMQTTClient.configureEndpoint("a1t8x9a9l0hxhj-ats.iot.us-east-2.amazonaws.com", 8883)

myMQTTClient.configureCredentials("/home/pi/awstest/AmazonRootCA1.pem", "/home/pi/awstest/6889f5cfb0-private.pem.key", "/home/pi/awstest/6889f5cfb0-certificate.pem.crt")

myMQTTClient.configureOfflinePublishQueueing(-1)  # Infinite offline Publish queueing
myMQTTClient.configureDrainingFrequency(2)  # Draining: 2 Hz
myMQTTClient.configureConnectDisconnectTimeout(10)  # 10 sec
myMQTTClient.configureMQTTOperationTimeout(5)  # 5 sec

myMQTTClient.connect()

# Add OAuth2 access token here.
# You can generate one for yourself in the App Console.
# See <https://blogs.dropbox.com/developers/2014/05/generate-an-access-token-for-your-own-account/>
TOKEN = 'HxP8iNTvrGAAAAAAAAAADkq2M5hCsLhqdp1UrmrcjJgOUf30tTbe9S8dNKJDILtz'

LOCALFILE = 'image.jpg'
BACKUPPATH = '/image.jpg'

kit = ServoKit(channels = 16)
kit.continuous_servo[0].set_pulse_width_range(700, 2300)

# construct the argument parser and parse the arguments
ap = argparse.ArgumentParser()
ap.add_argument("-v", "--video", type=str,
    help="path to input video file")
ap.add_argument("-t", "--tracker", type=str, default="mosse",
    help="OpenCV object tracker type")
args = vars(ap.parse_args())

dataout = None

def customCallback(client, userdata, message):
    print("Received a new message: ")
    print(message.payload)
    print("from topic: ")
    print(message.topic)
    print("--------------\n\n")
    
    customCallback.dataout = json.loads(message.payload)
    customCallback.dataout = customCallback.dataout['Coordinates']
    print(customCallback.dataout)

def backup():
    with open(LOCALFILE, 'rb') as f:
        # We use WriteMode=overwrite to make sure that the settings in the file
        # are changed on upload
        print("Uploading " + LOCALFILE + " to Dropbox as " + BACKUPPATH + "...")
        try:
            dbx.files_upload(f.read(), BACKUPPATH, mode=WriteMode('overwrite'))
        except ApiError as err:
            # This checks for the specific error where a user doesn't have
            # enough Dropbox space quota to upload this file
            if (err.error.is_path() and
                    err.error.get_path().reason.is_insufficient_space()):
                sys.exit("ERROR: Cannot back up; insufficient space.")
            elif err.user_message_text:
                print(err.user_message_text)
                sys.exit()
            else:
                print(err)
                sys.exit()
                
myMQTTClient.subscribe(topic, 1, customCallback)

# extract the OpenCV version info
(major, minor) = cv2.__version__.split(".")[:2]
 
# if we are using OpenCV 3.2 OR BEFORE, we can use a special factory
# function to create our object tracker
if int(major) == 3 and int(minor) < 3:
    tracker = cv2.Tracker_create(args["tracker"].upper())
 
# otherwise, for OpenCV 3.3 OR NEWER, we need to explicity call the
# approrpiate object tracker constructor:
else:
    # initialize a dictionary that maps strings to their corresponding
    # OpenCV object tracker implementations
    OPENCV_OBJECT_TRACKERS = {
        "mosse": cv2.TrackerMOSSE_create,
        "csrt": cv2.TrackerCSRT_create,
        "kcf": cv2.TrackerKCF_create,
        "boosting": cv2.TrackerBoosting_create,
        "mil": cv2.TrackerMIL_create,
        "tld": cv2.TrackerTLD_create,
        "medianflow": cv2.TrackerMedianFlow_create,
    }
 
    # grab the appropriate object tracker using our dictionary of
    # OpenCV object tracker objects
    tracker = OPENCV_OBJECT_TRACKERS[args["tracker"]]()
 
# initialize the bounding box coordinates of the object we are going
# to track
initBB = None

# if a video path was not supplied, grab the reference to the web cam
if not args.get("video", False):
    print("[INFO] starting video stream...")
#    try:
#        vs = VideoStream(usePiCamera=True).start()
#        print("Using Raspberry Pi Camera")
#    except:
#        vs = VideoStream(src=0).start()
#        print("Using USB Camera")
    vs = VideoStream(usePiCamera=True).start()
    print("Using Raspberry Pi Camera")
    time.sleep(1.0)
 
# otherwise, grab a reference to the video file
else:
    vs = cv2.VideoCapture(args["video"])
 
# initialize the FPS throughput estimator
fps = None
failures = 0

# loop over frames from the video stream
while True:
    # grab the current frame, then handle if we are using a
    # VideoStream or VideoCapture object
    frame = vs.read()
    frame = frame[1] if args.get("video", False) else frame
 
    # check to see if we have reached the end of the stream
    if frame is None:
        break
 
    # resize the frame (so we can process it faster) and grab the
    # frame dimensions
    frame = imutils.resize(frame, width=500)
    (H, W) = frame.shape[:2]
    # check to see if we are currently tracking an object
    
    if initBB is not None:
        # grab the new bounding box coordinates of the object
        (success, box) = tracker.update(frame)
 
        # check to see if the tracking was a success
        if success:
            failures = 0
            (x, y, w, h) = [int(v) for v in box]
            cv2.rectangle(frame, (x, y), (x + w, y + h),
                (0, 255, 0), 2)
            x_pos = x + w/2
            print(x_pos)
            if x_pos > 250 + w/2:
                kit.continuous_servo[0].throttle = -.01
                
            elif x_pos < 250 - w/2:
                kit.continuous_servo[0].throttle = .01
            
            else:
                kit.continuous_servo[0].throttle = 0
                
            print(kit.continuous_servo[0].throttle)
        else:
            failures = failures + 1
            print("Failures: " + str(failures))
 
 
        # update the FPS counter
        fps.update()
        fps.stop()
 
        # initialize the set of information we'll be displaying on
        # the frame
        info = [
            ("Tracker", args["tracker"]),
            ("Success", "Yes" if success else "No"),
            ("FPS", "{:.2f}".format(fps.fps())),
        ]
 
        # loop over the info tuples and draw them on our frame
        for (i, (k, v)) in enumerate(info):
            text = "{}: {}".format(k, v)
            cv2.putText(frame, text, (10, H - ((i * 20) + 20)),
                cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 255), 2)
        print(box)
    # show the output frame
    cv2.imshow("Frame", frame)
    key = cv2.waitKey(1) & 0xFF
    
    if failures > 10:
        print("Failure! Please press s to select new ROI")
        kit.continuous_servo[0].throttle = 0
        while(key != ord("s")):
            frame = vs.read()
            frame = frame[1] if args.get("video", False) else frame
         
            # check to see if we have reached the end of the stream
            if frame is None:
                break
         
            # resize the frame (so we can process it faster) and grab the
            # frame dimensions
            frame = imutils.resize(frame, width=500)
            (H, W) = frame.shape[:2]
            cv2.imshow("Frame", frame)
            key = cv2.waitKey(1) & 0xFF
        
 
    # if the 's' key is selected, we are going to "select" a bounding
    # box to track
    if key == ord("s") or failures > 10:
        cv2.imwrite('image.jpg', frame)
        #response = requests.post('https://content.dropboxapi.com/2/files/upload', headers=headers, data=data)
        if (len(TOKEN) == 0):
            sys.exit("ERROR: Looks like you didn't add your access token. "
            "Open up backup-and-restore-example.py in a text editor and "
            "paste in your token in line 14.")

        # Create an instance of a Dropbox class, which can make requests to the API.
        print("Creating a Dropbox object...")
        dbx = dropbox.Dropbox(TOKEN)

        # Check that the access token is valid
        try:
            dbx.users_get_current_account()
        except AuthError:
            sys.exit("ERROR: Invalid access token; try re-generating an "
                "access token from the app console on the web.")

        # Create a backup of the current settings file
        backup()
        print('Image dropped')
        
        # select the bounding box of the object we want to track (make
        # sure you press ENTER or SPACE after selecting the ROI)
        failures = 0
        kit.continuous_servo[0].throttle = 0
        # initBB = cv2.selectROI("Frame", frame, fromCenter=False,
            # showCrosshair=True)
        while(1):
            try:
                coordinates = customCallback.dataout.split(',')
                break
            except:
                pass
        print(coordinates)
        coordinates = [int(coordinate) for coordinate in coordinates]
        initBB = (coordinates[0], coordinates[1], coordinates[2] - coordinates[0], coordinates[3] - coordinates[1])
        print(initBB)
 
        # start OpenCV object tracker using the supplied bounding box
        # coordinates, then start the FPS throughput estimator as well
        tracker = OPENCV_OBJECT_TRACKERS[args["tracker"]]()
        tracker.init(frame, initBB)
        fps = FPS().start()
        

        # if the `q` key was pressed, break from the loop
    elif key == ord("q"):
        break
 
# if we are using a webcam, release the pointer
if not args.get("video", False):
    vs.stop()
 
# otherwise, release the file pointer
else:
    vs.release()
 
# close all windows
cv2.destroyAllWindows()
kit.continuous_servo[0].throttle = 0
