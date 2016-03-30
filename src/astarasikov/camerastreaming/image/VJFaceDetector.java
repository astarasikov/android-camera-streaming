package astarasikov.camerastreaming.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import astarasikov.camerastreaming.R;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class VJFaceDetector {
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
        List<Stage> stages = new LinkedList<Stage>();
    }

    static class Stage {
        double threshold;
        List<Feature> features = new LinkedList<Feature>();
    }

    static class Feature {
        double leftVal;
        double rightVal;
        double threshold;
        List<Rect> rects = new LinkedList<Rect>();
    }

    static class Rect {
        int x;
        int y;
        int width;
        int height;
        Double weight;
    }

    Context mContext;
    int mMaxFaces;
    Cascade mCascade;

    final double ITER_SCALE = 1.2;
    final double STDDEV_MIN = 10.0;
    final double STDDEV_FACE = 25.0;

    void processRects(Feature feature, Node featureSubnode) {
        for (Node child = featureSubnode.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!child.getNodeName().equals("rects")) {
                continue;
            }

            Node rect = child.getFirstChild();
            while (rect != null) {
                if (rect.getNodeName().equals("_")) {
                    Rect _rect = new Rect();
                    String rectS = rect.getTextContent().trim();
                    String data[] = rectS.split(" ");
                    _rect.x = Integer.valueOf(data[0]);
                    _rect.y = Integer.valueOf(data[1]);
                    _rect.width = Integer.valueOf(data[2]);
                    _rect.height = Integer.valueOf(data[3]);
                    _rect.weight = Double.valueOf(data[4]);
                    feature.rects.add(_rect);
                }
                rect = rect.getNextSibling();
            }
        }
    }

    void processFeatures(Stage stage, Node featureNode) {
        NodeList featureAttrs = featureNode.getChildNodes();
        int count = featureAttrs.getLength();

        Feature _feature = new Feature();

        for (int i = 0; i < count; i++) {
            Node child = featureAttrs.item(i);
            String key = child.getNodeName();
            String value = child.getTextContent().trim();

            if (key.equals("feature")) {
                processRects(_feature, child);
            } else if (key.equals("threshold")) {
                _feature.threshold = Double.valueOf(value);
            } else if (key.equals("left_val")) {
                _feature.leftVal = Double.valueOf(value);
            } else if (key.equals("right_val")) {
                _feature.rightVal = Double.valueOf(value);
            }
        }
        stage.features.add(_feature);
    }

    void processTreeNode(Stage stage, Node treeNode) {
        if (!treeNode.getNodeName().equals("_")) {
            return;
        }

        Node feature = treeNode.getFirstChild();
        while (feature != null) {
            processFeatures(stage, feature);
            feature = feature.getNextSibling();
        }
    }

    void processStage(Node stage, Cascade c) {
        if (!stage.getNodeName().equals("_")) {
            return;
        }

        NodeList stageNodes = stage.getChildNodes();
        int count = stageNodes.getLength();

        Stage _stage = new Stage();

        for (int i = 0; i < count; i++) {
            Node child = stageNodes.item(i);
            String key = child.getNodeName();
            String value = child.getTextContent().trim();

            if (key.equals("stage_threshold")) {
                _stage.threshold = Double.valueOf(value);
            } else if (key.equals("trees")) {
                Node tree = child.getFirstChild();
                while (tree != null) {
                    processTreeNode(_stage, tree);
                    tree = tree.getNextSibling();
                }
            }
        }
        c.stages.add(_stage);
    }

    void processCascade(Node node, Cascade c) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            String name = n.getNodeName();
            if (name.equals("size")) {
                String sizes[] = n.getTextContent().trim().split(" ");
                c.width = Integer.valueOf(sizes[0]);
                c.height = Integer.valueOf(sizes[1]);

                if (c.width == 0) {
                    c.width = 20;
                }
                if (c.height == 0) {
                    c.height = 20;
                }
            } else if (name.equals("stages")) {
                Node stage = n.getFirstChild();
                while (stage != null) {
                    processStage(stage, c);
                    stage = stage.getNextSibling();
                }
            }
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

        Node storageNode = dom.getFirstChild();
		while (!storageNode.getNodeName().startsWith("opencv_storage")) {
			storageNode = storageNode.getNextSibling();
		}
        NodeList cascades = storageNode.getChildNodes();
        Node cascadeNode = null;
        for (int i = 0; i < cascades.getLength(); i++) {
            Node cascade = cascades.item(i);
            if (cascade.getNodeName().startsWith("haarcascade")) {
                cascadeNode = cascade;
                break;
            }
        }

        mCascade = new Cascade();
        processCascade(cascadeNode, mCascade);
    }

    byte[] buildGrayScale(int[] pixels) {
        byte[] grayScale = new byte[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            //just take the gren channel for now
            grayScale[i] = (byte) ((pixels[i] >> 8) & 0xff);
        }
        return grayScale;
    }

    void buildIntegralImage(byte[] in, int[][] dst, int w, int h) {
        int[] out = new int[in.length];
        int[] sqr = new int[in.length];

        for (int i = 0; i < h; i++) {
            int offset = i * w;
            for (int j = 0; j < w; j++) {
                int idx = offset + j;
                out[idx] = in[idx];
                sqr[idx] = in[idx] * in[idx];

                if (i > 0 && j > 0) {
                    int idx_iminus_jminus = idx - w - 1;
                    out[idx] -= out[idx_iminus_jminus];
                    sqr[idx] -= sqr[idx_iminus_jminus];
                }
                if (i > 0) {
                    int idx_iminus = idx - w;
                    out[idx] += out[idx_iminus];
                    sqr[idx] += sqr[idx_iminus];
                }
                if (j > 0) {
                    int idx_jminus = idx - 1;
                    out[idx] += out[idx_jminus];
                    sqr[idx] += sqr[idx_jminus];
                }
            }
        }
        dst[0] = out;
        dst[1] = sqr;
    }

    int sum(int[] arr, int x, int y, int w, int h, int imageWidth, int imageHeight) {
        if (x + w >= imageWidth || y + h >= imageHeight) {
            return 0;
        }

        int i1 = imageWidth * y + x;
        int i2 = imageWidth * (y + h) + x;

        int s1 = arr[i1];
        int s2 = arr[i2];
        int s3 = arr[i1 + w];
        int s4 = arr[i2 + w];
        return s4 + s1 - s2 - s3;
    }

    int detect(int pixels[], int width, int height, VJFace faces[]) {
        int[][] integrals = new int[2][];
        byte[] grayScale = buildGrayScale(pixels);
        buildIntegralImage(grayScale, integrals, width, height);
        int[] integral = integrals[0];
        int[] int_sqr = integrals[1];

        int win_h = mCascade.height;
        int win_w = mCascade.width;

        double win_scale = 1.0;
        int faceIndex = 0;

        //scale image for each
        while (Math.min(win_h, win_w) < Math.min(width, height)) {
            double win_norm = 1.0 / (win_w * win_h);

            int w_step_y = Math.max(1, Math.min(4, win_h / 10));
            int w_step_x = Math.max(1, Math.min(4, win_w / 10));

            //slide the classifier window across the image
            for (int wy = 0; wy < height - win_h; wy += w_step_y) {
                for (int wx = 0; wx < width - win_w; wx += w_step_x) {
                    int w_sum = sum(integral, wx, wy, win_w, win_h, width, height);
                    int w_ssq = sum(int_sqr, wx, wy, win_w, win_h, width, height);

                    double stddev = 1.0;
                    double mean = w_sum * win_norm;
                    double var = (w_ssq * win_norm) - (mean * mean);
                    if (var >= 0) {
                        stddev = Math.sqrt(var);
                    }
                    if (stddev < STDDEV_MIN) {
                        continue;
                    }

                    boolean failed = false;

                    for (Stage s : mCascade.stages) {
                        double sum_stage = 0.0;
                        for (Feature f : s.features) {
                            double sum_feature = 0.0;
                            for (Rect r : f.rects) {
                                int rx = (int)(r.x * win_scale);
                                int ry = (int)(r.y * win_scale);
                                int rw = (int)(r.width * win_scale);
                                int rh = (int)(r.height * win_scale);
                                sum_feature += sum(integral, wx + rx, wy + ry,
                                        rw, rh, width, height) * r.weight;
                            } //Rects
                            if (sum_feature * win_norm < f.threshold * stddev) {
                                sum_stage += f.leftVal;
                            } else {
                                sum_stage += f.rightVal;
                            }
                        } //Features

                        if (sum_stage < s.threshold) {
                            failed = true;
                            break;
                        }
                    } //Stages

                    if (!failed) {
                        if (stddev > STDDEV_FACE) {
                            if (faceIndex >= mMaxFaces) {
                                return faces.length;
                            }
                            Log.i("ViolaJones", String.format("Face [%d %d %d %d]", wx, wy, win_w, win_h));
                            VJFace face = new VJFace(wx + (win_w / 2), wy + (win_h / 2), (int)(win_w * 0.8));
                            faces[faceIndex++] = face;
                        }
                    }
                }
            } //scale window

            win_h *= ITER_SCALE;
            win_w *= ITER_SCALE;
            win_scale *= ITER_SCALE;
        }

        return faceIndex;
    }

    public VJFaceDetector(Context context, int width, int height, int maxFaces)
            throws Exception {
        this.mContext = context;
        this.mMaxFaces = maxFaces;
        loadCascade();
    }

    public int findFaces(Bitmap bitmap, VJFace faces[]) {
        Log.i("ViolaJones", "findFaces!");
        int mWidth = bitmap.getWidth();
        int mHeight = bitmap.getHeight();
        int mPixels[] = new int[mWidth * mHeight];
        bitmap.getPixels(mPixels, 0, mWidth, 0, 0, mWidth, mHeight);
        Log.i("ViolaJones", "Bitmap " + mWidth + " " + mHeight);
        return detect(mPixels, mWidth, mHeight, faces);
    }
}
