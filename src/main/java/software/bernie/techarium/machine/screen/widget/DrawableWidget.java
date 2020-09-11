package software.bernie.techarium.machine.screen.widget;

import javafx.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.Widget;
import software.bernie.techarium.client.screen.draw.IDrawable;
import software.bernie.techarium.machine.controller.MachineController;

public class DrawableWidget extends Widget {

    private IDrawable drawable;
    private final int sizeX;
    private final int sizeY;
    private MachineController controller;

    public DrawableWidget(MachineController controller, IDrawable drawable, int xIn, int yIn, int sizeX, int sizeY, String msg) {
        super(xIn, yIn, msg);
        this.drawable = drawable;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.controller = controller;
    }

    private Pair<Integer, Integer> getBackgroundSize() {
        return controller.getBackgroundSizeXY();
    }

    @Override
    public void renderButton(int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenY = minecraft.getMainWindow().getScaledHeight() / 2;
        int screenX = minecraft.getMainWindow().getScaledWidth() / 2;
        drawable.draw(screenX - getBackgroundSize().getValue() / 2 + x, screenY - getBackgroundSize().getKey() / 2 + y, sizeX, sizeY);
    }
}
