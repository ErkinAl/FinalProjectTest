from ultralytics import YOLO

# YOLOv8 pose modelini yükle
model = YOLO("yolov8m-pose.pt")
# ONNX formatına dönüştür
model.export(format="onnx", dynamic=True)