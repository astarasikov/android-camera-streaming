package astarasikov.camerastreaming.image;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import astarasikov.camerastreaming.R;

public class VJFaceDetector
{
	class VJFace {
		public final int centerX;
		public final int centerY;
		public final int eyesDistance;
		
		public VJFace(int centerX, int centerY, int eyesDistance) {
			this.centerX = centerX;
			this.centerY = centerY;
			this.eyesDistance = eyesDistance;
		}
	}
	
	static class Cascade {
		int width;
		int height;
		List<Stage> stages;
	}

	static class Stage {
		double threshold;
		List<Feature> features;
	}

	static class Feature {
		double leftVal;
		double rightVal;
		double threshold;
		List<Rect> rects;
	}

	static class Rect {
		int x;
		int y;
		int width;
		int heigth;
		int weight;
	}
	
	Context mContext;
	int mMaxFaces;
	int mPixels[];
	int integralImage[];
	int integralSquares[];
	int mWidth;
	int mHeight;

	Cascade cascade;	
	byte grayscale[];

	VJFace mFaces[];
	
	
	void processRects(Feature feature, Node featureSubnode) {
		NodeList featureSubAttrs = featureSubnode.getChildNodes();
		int count = featureSubAttrs.getLength();
		
		for (int i = 0; i < count; i++) {
			Node child = featureSubAttrs.item(i);
			if (child.getNodeName().equals("rects")) {
				NodeList rects = child.getChildNodes();
				int rectCount = rects.getLength();
				for (int j = 0; j < rectCount; j++) {
					Node rect = rects.item(i);
					if (rect.getNodeName().equals("_")) {
						Rect _rect = new Rect();
						String rectS = rect.getTextContent();
						rectS = rectS.substring(0, rectS.length() - 1);
						String data[] = rectS.split(" ");
						_rect.x = Integer.valueOf(data[0]);
						_rect.y = Integer.valueOf(data[1]);
						_rect.width = Integer.valueOf(data[2]);
						_rect.heigth = Integer.valueOf(data[3]);
						_rect.weight = Integer.valueOf(data[4]);
						feature.rects.add(_rect);
					}
				}
			}
		}
	}
	
	void processFeatures(Stage stage, Node featureNode) {
		NodeList featureAttrs = featureNode.getChildNodes();
		int count = featureAttrs.getLength();
		
		Feature _feature = new Feature();
		_feature.rects = new LinkedList<Rect>();

		for (int i = 0; i < count; i++) {
			Node child = featureAttrs.item(i);
			String key = child.getNodeName();
			String value = child.getTextContent();
			
			if (key.equals("feature")) {
				processRects(_feature, child);
			}
			else if (key.equals("threshold")) {
				_feature.threshold = Double.valueOf(value);
			}
			else if (key.equals("left_val")) {
				_feature.leftVal = Double.valueOf(value);
			}
			else if (key.equals("right_val")) {
				_feature.rightVal = Double.valueOf(value);
			}
		}
		stage.features.add(_feature);
	}
	
	void processTreeNode(Stage stage, Node treeNode) {
		NodeList contents = treeNode.getChildNodes();
		int count = contents.getLength();
		for (int i = 0; i < count; i++) {
			Node feature = contents.item(i);
			if (feature.getNodeName().equals("_")) {
				processFeatures(stage, feature);
			}
		}	
	}
	
	void processTrees(Stage stage, Node stageNode) {
		NodeList treeNodes = stageNode.getChildNodes();
		int count = treeNodes.getLength();
		for (int i = 0; i < count; i++) {
			Node treeContents = treeNodes.item(i);
			processTreeNode(stage, treeContents);
		}
	}

	void processStage(Node stage) {
		NodeList stageNodes = stage.getChildNodes();
		int count = stageNodes.getLength();

		Stage _stage = new Stage();
		_stage.features = new LinkedList<Feature>();

		for (int i = 0; i < count; i++) {
			Node child = stageNodes.item(i);
			String key = child.getNodeName();
			String value = child.getTextContent();
			
			if (key.equals("stage_threshold")) {
				_stage.threshold = Double.valueOf(value);
			}
			else if (key.equals("trees")) {
				processTrees(_stage, child);
			}
		}
		cascade.stages.add(_stage);
	}

	void processStages(NodeList nodes) {
		int count = nodes.getLength();
		for (int i = 0; i < count; i++) {
			Node node = nodes.item(i);
			if (node.getNodeName() != "_") {
				continue;
			}
			processStage(node);
		}
	}
	
	void loadCascade() throws Exception {
		InputStream is =
				mContext
				.getResources()
				.openRawResource(R.raw.haarcascade_frontalface_alt);

		DocumentBuilderFactory factory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document dom = builder.parse(is);
		NodeList cascades = dom
				.getElementsByTagName("haarcascade_frontalface_alt");
		Node cascadeNode = cascades.item(0);
		NodeList cascadeNodes = cascadeNode.getChildNodes();
		
		cascade = new Cascade();
		cascade.stages = new LinkedList<Stage>();
		
		for (int i = 0; i < cascadeNodes.getLength(); i++) {
			Node node = cascadeNodes.item(i);

			String nodeName = node.getNodeName();
			if (nodeName.equals("size")) {
				String nodeVal = node.getTextContent();
				String sizes[] = nodeVal.split(" ");
				cascade.width = Integer.valueOf(sizes[0]);
				cascade.height = Integer.valueOf(sizes[1]);

			} else if (nodeName.equals("stages")) {
				processStages(node.getChildNodes());
			}
		}
	}
	
	void emitFace(int idx, int x0, int y0, int x1, int y1) {
		if (idx >= mFaces.length) {
			return;
		}
		mFaces[idx] = new VJFace(x0, x1, x1 - x0);
	}
	
	void buildGrayScale() {
		grayscale = new byte[mPixels.length];
		for (int i = 0; i < mPixels.length; i++) {
			//just take the gren channel for now
			grayscale[i] = (byte)((mPixels[i] >> 8) & 0xff);
		}
	}
	
	void buildIntegralImage() {
		integralImage = new int[grayscale.length];
		for (int i = 0; i < mHeight; i++) {
			for (int j = 0; j < mWidth; j++) {
				int idx = i * mWidth + j;
				int idx1 = idx - 1 - mWidth;
				int idx2 = idx - mWidth;
				int idx3 = idx - 1;
				
				int i0 = grayscale[idx];
				int i1 = idx1 >= 0 ? grayscale[idx1] : 0;
				int i2 = idx2 >= 0 ? grayscale[idx2] : 0;
				int i3 = idx3 >= 0 ? grayscale[idx3] : 0;
				
				integralImage[idx] = i0 - i1 + i2 + i3;
			}
		}
	}
	
	void buildIntegralSquares() {
		integralSquares = new int[grayscale.length];
		for (int i = 0; i < integralSquares.length; i++) {
			integralSquares[i] = (int)(integralImage[i] * integralImage[i]);
		}
	}
	
	double sum(int x0, int y0, int x1, int y1) {
		int A = integralImage[x0 * mWidth + y1];
		int B = integralImage[x0 * mWidth + y0];
		int C = integralImage[x1 * mWidth + y0];
		int D = integralImage[x1 * mWidth + y1];

		return (A + C - B - D) / 256.0;
	}
	
	double sumSquares(int x0, int y0, int x1, int y1) {
		int A = integralSquares[x0 * mWidth + y1];
		int B = integralSquares[x0 * mWidth + y0];
		int C = integralSquares[x1 * mWidth + y0];
		int D = integralSquares[x1 * mWidth + y1];

		return (A + C - B - D) / (256.0 * 256.0);
	}
	
	int detect(int pixels[], int width, int height) {
		mPixels = pixels;
		mWidth = width;
		mHeight = height;
		buildGrayScale();
		buildIntegralImage();
		buildIntegralSquares();
				
		int window_w_base = cascade.width;
		int window_h_base = cascade.height;
		
		double factor = 1.2;
		double win_scale = 1.0;
		
		int window_w;
		int window_h;
		
		int faceCount = 0;
		boolean failed = false;
		do {
			window_w = (int)(window_w_base * win_scale);
			window_h = (int)(window_h_base * win_scale);
			
			if (window_w >= mWidth || window_h >= mHeight) {
				break;
			}
			
			int x_step = Math.max(1, Math.min(4, window_w / 8));
			int y_step = Math.max(1, Math.min(4, window_h / 8));
			
			double inv_norm = 1.0 / (window_h * window_w);
			double stddev;
			
			for (int y1 = 0; y1 < mHeight - window_h; y1 += y_step) {
				for (int x1 = 0; x1 < mWidth - window_w; x1 += x_step) {
					double mean = sum(x1, y1, x1 + window_w, y1 + window_h) * inv_norm;
					double var = 
							sumSquares(x1, y1, x1 + window_w, y1 + window_h) * inv_norm 
							- mean * mean;
					if (var >= 0.0) {
						stddev = Math.sqrt(var);
					}
					else {
						stddev = 1.0;
					}
					
					//if (stddev < 10.0) {
					//	continue;
					//}
					
					for (Stage s : cascade.stages) {
						double sum_stage = 0.0;
						for (Feature f : s.features) {
							double sum_feature = 0.0;
							for (Rect r : f.rects) {
								/*int _x0 = x1 + r.x;
								int _y0 = y1 + r.y;
								int _w = r.width;
								int _h = r.heigth;*/
								int _x0 = (int)(x1 + r.x * inv_norm);
								int _y0 = (int)(y1 + r.y * inv_norm);
								int _w = (int)(r.width * inv_norm + 2);
								int _h = (int)(r.heigth * inv_norm + 2);
								sum_feature +=
										sum(_x0, _y0, _x0 + _w, _y0 + _h) * r.weight;
							}
							sum_feature *= inv_norm;
							if (sum_feature < f.threshold * stddev) {
								sum_stage += f.leftVal;
							}
							else {
								sum_stage += f.rightVal;
							}			
						}
						if (sum_stage < s.threshold) {
							failed = true;
							break;
						}
					}
					win_scale *= factor;
					if (!failed) {
						emitFace(faceCount, x1, y1, x1 + window_w, y1 + window_h);
						faceCount++;
						if (faceCount >= mMaxFaces) {
							break;
						}
					}
				}
			}
		} while (Math.min(mWidth, mHeight) >= Math.min(window_w, window_h)
				&& faceCount < mMaxFaces);
		return Math.min(faceCount, mFaces.length);
	}
	
	public VJFaceDetector(Context context, int width, int height, int maxFaces)
			throws Exception
	{
		this.mContext = context;
		this.mWidth = width;
		this.mHeight = height;
		this.mMaxFaces = maxFaces;
		this.mPixels = new int[mWidth * mHeight];
		loadCascade();
	}
	
	public int findFaces(Bitmap bitmap, VJFace faces[]) {
		mFaces = faces;
		bitmap.getPixels(mPixels, 0, mWidth, 0, 0, mWidth, mHeight);
		return detect(mPixels, mWidth, mHeight);
	}
}
