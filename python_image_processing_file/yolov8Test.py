from ultralytics import YOLO

# YOLOv8 pose modelini yükle
model = YOLO("yolov8n-pose.pt")
model.export(format="onnx", dynamic=True)