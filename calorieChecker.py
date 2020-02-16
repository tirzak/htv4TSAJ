import webcolors
theStuffList = []
theColorList = []
cals=0
def localize_objects(path):
    """Localize objects in the local image.

    Args:
    path: The path to the local file.
    """
    from google.cloud import vision
    client = vision.ImageAnnotatorClient()

    with open(path, 'rb') as image_file:
        content = image_file.read()
    image = vision.types.Image(content=content)

    objects = client.object_localization(
        image=image).localized_object_annotations
    
    response = client.image_properties(image=image)
    props = response.image_properties_annotation
    for color in props.dominant_colors.colors:
        cTriplet = (int(color.color.red), int(color.color.green), int(color.color.blue))
        #print (webcolors.rgb_to_name(str((int(color.color.red), int(color.color.green), int(color.color.blue)))))
        #print(webcolors.rgb_to_name((int(color.color.red), int(color.color.green), int(color.color.blue))))
        #theColorList.append("(" + color.color.red + ", " + color.color.green + ", " + color.color.blue + ")")
        #print (webcolors.rgb_to_name(cTriplet))
        """print('frac: {}'.format(color.pixel_fraction))
        print('\tr: {}'.format(color.color.red))
        print('\tg: {}'.format(color.color.green))
        print('\tb: {}'.format(color.color.blue))"""
        #0 - (256*256*256)/7
        #(256*256*256)/7 - 2*(256*256*256)/7 orange

        break

    #print('Number of objects found: {}'.format(len(objects)))
    for object_ in objects:
        #print('\n{} (confidence: {})'.format(object_.name, object_.score))
        #print(object_.name)
        theStuffList.append(object_.name.lower())
        #print('Normalized bounding polygon vertices: ')
        #for vertex in object_.bounding_poly.normalized_vertices:
        #    print(' - ({}, {})'.format(vertex.x, vertex.y))

"""import sys
from google.cloud import automl_v1beta1
from google.cloud.automl_v1beta1.proto import service_pb2"""
import io, os, cv2 
from numpy import random
os.environ["GOOGLE_APPLICATION_CREDENTIALS"]="hackthevalley4-0bed0a0905ed.json"

#client = vision.ImageAnnotatorClient()
key = cv2. waitKey(1)
webcam = cv2.VideoCapture(0)
while True:
    try:
        check, frame = webcam.read()
        cv2.imshow("Capturing", frame)
        key = cv2.waitKey(1)
        while key != ord('q'):
            try:
                check, frame = webcam.read()
                cv2.imshow("Capturing", frame)
                key = cv2.waitKey(1)
                if key == ord('s'): 
                    cv2.imwrite(filename='saved_img.jpg', img=frame)
                    img_new = cv2.imread('saved_img.jpg')
                    img_new = cv2.imshow("Captured Image", img_new)
                    cv2.waitKey(1650)
                    localize_objects("saved_img.jpg")
                elif key == ord('q'):
                    webcam.release()
                    cv2.destroyAllWindows()
                    for stuff in theStuffList:
                        if stuff == "tin can":
                            print("Soda")
                            cals+=140
                        elif stuff == "packaged goods":
                            print("Snack Bar")
                            cals+=100
                        elif stuff == "bottled and jarred packaged goods":
                            print("Apple Juice")
                            cals+=130
                        elif stuff == "muffin":
                            print("Muffin")
                            cals+=377
                        elif stuff == "bread":
                            print("Bread")
                            cals+=66
                        elif stuff == "apple":
                            print("Apple")
                            cals+=52
                        elif stuff == "pizza":
                            print("Pizza")
                            cals+=250
                        #print(stuff)
                    print("Total Calories: " + str(cals))
            except(KeyboardInterrupt):
                webcam.release()
                cv2.destroyAllWindows()
                break
    except(KeyboardInterrupt):
        webcam.release()
        cv2.destroyAllWindows()
        break







