from ultralytics import YOLO

model = YOLO("yolov8n-pose.pt")

results = model.track(source="test3.mp4", save=True)
print(results[0].save_dir)

#show=True