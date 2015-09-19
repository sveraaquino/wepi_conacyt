package py.fpuna.lcca.process;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class ColorOpening implements PlugInFilter {
	private int iteration;

	public boolean wepi_setup(Object... objects) {
		iteration = (Integer) objects[0];
		if (iteration <= 0)
			iteration = 1;
		return true;
	}

	public ImageProcessor wepi_run(ImageProcessor ip, Object... objects) {
		if (!wepi_setup(objects))
			return null;
		ImageProcessor ip2 = ip.duplicate();
		for (int i = 0; i < iteration; i++)
			ip2.erode();
		for (int i = 0; i < iteration; i++)
			ip2.dilate();
		return ip2;
	}

	public void run(ImageProcessor ip) {
		ImagePlus imp = new ImagePlus("Opening", ip);
		ColorProcessor cp = (ColorProcessor) imp.getProcessor();
		for (int i = 0; i < this.iteration; i++)
			cp.erode();
		for (int i = 0; i < this.iteration; i++)
			cp.dilate();
		imp.setProcessor(cp);
		imp.show();
	}

	public int setup(String str, ImagePlus imp) {
		if (imp == null)
			return DONE;
		else if (setupWindow())
			return DONE;
		return DOES_ALL;
	}

	private boolean setupWindow() {
		GenericDialog gd = new GenericDialog("Opening");
		gd.addNumericField("Iteration value: ", 1, 0);
		gd.showDialog();
		this.iteration = (int) gd.getNextNumber();
		if (this.iteration <= 0)
			return false;
		return true;
	}

}