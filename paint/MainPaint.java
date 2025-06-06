package paint;

import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.listener.PaintListener;

import java.awt.*;

public class MainPaint implements PaintListener {

    private final AbstractScript script;

    public MainPaint(AbstractScript script) {
        this.script = script;
    }

    @Override
    public void onPaint(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(10, 10, 200, 60, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString("EliteBot - Running", 20, 30);
        g.drawString("Runtime: " + script.getRuntime(), 20, 50);
    }
}