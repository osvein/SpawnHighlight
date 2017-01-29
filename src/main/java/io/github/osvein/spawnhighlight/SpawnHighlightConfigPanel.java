package io.github.osvein.spawnhighlight;

import com.mumfrey.liteloader.client.gui.GuiCheckbox;
import com.mumfrey.liteloader.modconfig.AbstractConfigPanel;
import com.mumfrey.liteloader.modconfig.ConfigPanelHost;

import net.minecraft.client.resources.I18n;

public class SpawnHighlightConfigPanel extends AbstractConfigPanel
{
    @Override
    public String getPanelTitle()
    {
        return I18n.format("spawnhighlight.config.title");
    }
    
    @Override
    protected void addOptions(ConfigPanelHost host)
    {
        final LiteModSpawnHighlight mod = host.<LiteModSpawnHighlight>getMod();
        
        this.addControl(new GuiCheckbox(0, 0, 0, I18n.format("spawnhighlight.config.option.enabled")), new ConfigOptionListener<GuiCheckbox>() {
            @Override
            public void actionPerformed(GuiCheckbox control) {
                mod.visible = (control.checked = !control.checked);
            }
        }).checked = mod.visible;
        this.addControl(new GuiCheckbox(1, 0, 16, I18n.format("spawnhighlight.config.option.topmost")), new ConfigOptionListener<GuiCheckbox>() {
			@Override
			public void actionPerformed(GuiCheckbox control) {
				mod.topmost = (control.checked = !control.checked);
			}
		}).checked = mod.topmost;
        this.addControl(new GuiCheckbox(2, 0, 32, I18n.format("spawnhighlight.config.option.skylight")), new ConfigOptionListener<GuiCheckbox>() {
			@Override
			public void actionPerformed(GuiCheckbox control) {
				mod.skylight = (control.checked = !control.checked);
			}
		}).checked = mod.skylight;
        this.addControl(new GuiCheckbox(3, 0, 48, I18n.format("spawnhighlight.config.option.antialias")), new ConfigOptionListener<GuiCheckbox>() {
			@Override
			public void actionPerformed(GuiCheckbox control) {
				mod.antialias = (control.checked = !control.checked);
			}
		}).checked = mod.antialias;
    }

    @Override
    public void onPanelHidden() {}
}
