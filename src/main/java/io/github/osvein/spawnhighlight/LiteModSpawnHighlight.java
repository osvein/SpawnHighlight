package io.github.osvein.spawnhighlight;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mumfrey.liteloader.Configurable;
import com.mumfrey.liteloader.PostRenderListener;
import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.gl.GL;
import com.mumfrey.liteloader.modconfig.ConfigPanel;
import com.mumfrey.liteloader.modconfig.ConfigStrategy;
import com.mumfrey.liteloader.modconfig.ExposableOptions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving.SpawnPlacementType;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.WorldEntitySpawner;

@ExposableOptions(strategy = ConfigStrategy.Versioned, filename="spawnhighlight.json")
public class LiteModSpawnHighlight implements Tickable, PostRenderListener, Configurable {
	public static final String NAME = "SpawnHighlight";
	public static final String VERSION = "0.1";
	
	public static final float LINE_WIDTH = 3.0F;
	public static final float COLOR_R = 1.0F;
	public static final float COLOR_G = 0F;
	public static final float COLOR_B = 0F;
	public static final float COLOR_NR = 1.0F;
	public static final float COLOR_NG = 1.0F;
	public static final float COLOR_NB = 0F;
	public static final float OFFSET = 0.004F;
	
	protected static final KeyBinding highlightKeyBinding = new KeyBinding("spawnhighlight.key.visible", Keyboard.KEY_F12, "key.categories.litemods");
	
	public final int radius = 16;
	public final int lightThreshold = 8; // lowest safe light level
	public final int refreshInterval = 20;
	
	@Expose
	@SerializedName("visible")
	public boolean visible = false;
	
	@Expose
	@SerializedName("topmost")
	public boolean topmost = false;
	
	@Expose
	@SerializedName("skylight")
	public boolean skylight = true;
	
	@Expose
	@SerializedName("antialias")
	public boolean antialias = true;
	
	protected int refreshCountdown;
	
	protected final Set<Tuple<BlockPos, AxisAlignedBB>> unsafeBox = new HashSet<Tuple<BlockPos, AxisAlignedBB>>(radius*radius);
	protected final Set<Tuple<BlockPos, AxisAlignedBB>> unsafeBoxNight = new HashSet<Tuple<BlockPos, AxisAlignedBB>>(radius*radius);
	protected final Set<Tuple<BlockPos, AxisAlignedBB>> unsafeTop = new HashSet<Tuple<BlockPos, AxisAlignedBB>>(radius*radius);
	protected final Set<Tuple<BlockPos, AxisAlignedBB>> unsafeTopNight = new HashSet<Tuple<BlockPos, AxisAlignedBB>>(radius*radius);
	protected Entity renderViewEntity = null;
	
	@Override
	public String getName() {
		return LiteModSpawnHighlight.NAME;
	}
	
	@Override
	public String getVersion() {
		return LiteModSpawnHighlight.VERSION;
	}
	
	@Override
	public Class<? extends ConfigPanel> getConfigPanelClass() {
	    return SpawnHighlightConfigPanel.class;
	}
	
	@Override
	public void init(File configPath) {
		LiteLoader.getInput().registerKeyBinding(LiteModSpawnHighlight.highlightKeyBinding);
	}
	
	@Override
	public void upgradeSettings(String version, File configPath, File oldConfigPath) {}
	
	@Override
	public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock) {	
		if (inGame) {
			if (LiteModSpawnHighlight.highlightKeyBinding.isPressed()) {
				this.visible = !this.visible;
			}
			
			if (this.visible && --this.refreshCountdown <= 0) {
				refresh(minecraft, minecraft.getRenderViewEntity().getPosition(), this.radius);
				this.refreshCountdown = this.refreshInterval;
			}
		}
	}

	@Override
	public void onPostRenderEntities(float partialTicks) {
		if (this.visible && !this.topmost) {
			RenderHelper.disableStandardItemLighting();
			this.render(partialTicks);
			RenderHelper.enableStandardItemLighting();
		}
	}

	@Override
	public void onPostRender(float partialTicks) {
		if (this.visible && this.topmost) this.render(partialTicks);
	}
	
	public void refresh(Minecraft minecraft, BlockPos center, int radius) {
		this.refresh(minecraft, center, new Vec3i(radius, radius, radius));
	}
	
	public void refresh(Minecraft minecraft, BlockPos center, Vec3i radius) {
		this.refresh(minecraft, center.subtract(radius), center.add(radius));
	}
	
	public void refresh(Minecraft minecraft, BlockPos from, BlockPos to) {
		this.unsafeBox.clear();
		this.unsafeBoxNight.clear();
		this.unsafeTop.clear();
		this.unsafeTopNight.clear();
		
		/*
		int ymin = 0;
		int ymax = minecraft.world.getActualHeight();
		int y1 = MathHelper.clamp(from.getY(), ymin, ymax);
		int y2 = MathHelper.clamp(to.getY(), ymin, ymax);
		
		for (int x = from.getX(); x <= to.getX(); x++) {
			for (int z = from.getZ(); z <= to.getZ(); z++) {
				Biome biome = minecraft.world.getBiome(new BlockPos(x, ymin, z));
				if (biome.getSpawnableList(EnumCreatureType.MONSTER).isEmpty() || biome.getSpawningChance() <= 0) continue;
				
				for (int y = y1; y <= y2; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					if (WorldEntitySpawner.canCreatureTypeSpawnAtLocation(SpawnPlacementType.ON_GROUND, minecraft.world, pos) && minecraft.world.getLightFor(EnumSkyBlock.BLOCK, pos) < this.lightThreshold) {
						if (minecraft.world.getLightFor(EnumSkyBlock.SKY, pos) < this.lightThreshold) {
							// unsafe
							this.unsafe.add(pos);
						}
						else {
							// unsafe at night
							this.unsafeNight.add(pos);
						}
					}
				}
			}
		}*/
		
		// this method doesn't check biome properties
		int skylightSubtracted = minecraft.world.getSkylightSubtracted();
		minecraft.world.setSkylightSubtracted(0xF);
		for (BlockPos pos : BlockPos.getAllInBox(from, to)) {
			if (WorldEntitySpawner.canCreatureTypeSpawnAtLocation(SpawnPlacementType.ON_GROUND, minecraft.world, pos) && minecraft.world.getLightFromNeighbors(pos) < this.lightThreshold) {
				BlockPos posBelow = pos.down();
				AxisAlignedBB bb1 = minecraft.world.getBlockState(posBelow).getBoundingBox(minecraft.world, posBelow);
				AxisAlignedBB bb2 = minecraft.world.getBlockState(pos).getBoundingBox(minecraft.world, pos);
				BlockPos posTopToAdd = posBelow;
				AxisAlignedBB bbTopToAdd = bb1;
				Tuple<BlockPos, AxisAlignedBB> boxToAdd = null;
				if (bb2.minX > bb1.minX || bb2.maxX < bb1.maxX || bb2.minZ > bb1.minZ || bb2.maxZ < bb1.maxZ) {
					boxToAdd = new Tuple<BlockPos, AxisAlignedBB>(pos, bb2);
				}
				else if (!minecraft.world.isAirBlock(pos)) {
					posTopToAdd = pos;
					bbTopToAdd = bb2;
				}
				Tuple<BlockPos, AxisAlignedBB> topToAdd = new Tuple<BlockPos, AxisAlignedBB>(posTopToAdd, bbTopToAdd);
					
				minecraft.world.setSkylightSubtracted(skylightSubtracted);
				if (minecraft.world.getLightFromNeighbors(pos) < this.lightThreshold) {
					// unsafe
					this.unsafeTop.add(topToAdd);
					if (boxToAdd != null) this.unsafeBox.add(boxToAdd);
				}
				else if (this.skylight) {
					// unsafe at night
					this.unsafeTopNight.add(topToAdd);
					if (boxToAdd != null) this.unsafeBoxNight.add(boxToAdd);
				}
				minecraft.world.setSkylightSubtracted(0xF);
			}
		}
		minecraft.world.setSkylightSubtracted(skylightSubtracted);
	}
	
	public void render(float partialTicks) {
		Entity renderViewEntity = Minecraft.getMinecraft().getRenderViewEntity();
		double x = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * partialTicks;
		double y = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * partialTicks;
		double z = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * partialTicks;
		
		GL.glPushMatrix();
		GL.glTranslated(-x, -y, -z);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
		GL.glEnableBlend();
		GL.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		GL.glDisableTexture2D();
		GL.glDisableLighting();
		GL.glDisableFog();
		GL.glLineWidth(LiteModSpawnHighlight.LINE_WIDTH);
		if (this.antialias) GL11.glEnable(GL.GL_LINE_SMOOTH); else GL11.glDisable(GL.GL_LINE_SMOOTH);
		
		GlStateManager.glBegin(GL.GL_LINES);
		GL.glColor3f(COLOR_R, COLOR_G, COLOR_B);
		for (Tuple<BlockPos, AxisAlignedBB> top : this.unsafeTop) {
			this.renderTop(top.getFirst(), top.getSecond());
		}
		GL.glColor3f(COLOR_NR, COLOR_NG, COLOR_NB);
		for (Tuple<BlockPos, AxisAlignedBB> top : this.unsafeTopNight) {
			this.renderTop(top.getFirst(), top.getSecond());
		}
		GlStateManager.glEnd();
		
		
		GL.glColor4f(COLOR_R, COLOR_G, COLOR_B, 0.25f);
		for (Tuple<BlockPos, AxisAlignedBB> box : this.unsafeBox) {
			GlStateManager.glBegin(GL.GL_TRIANGLE_STRIP);
			this.renderBox(box.getFirst(), box.getSecond());
			GlStateManager.glEnd();
		}
		GL.glColor4f(COLOR_NR, COLOR_NG, COLOR_NB, 0.25f);
		for (Tuple<BlockPos, AxisAlignedBB> box : this.unsafeBoxNight) {
			GlStateManager.glBegin(GL.GL_TRIANGLE_STRIP);
			this.renderBox(box.getFirst(), box.getSecond());
			GlStateManager.glEnd();
		}
		
		GL.glColor3f(COLOR_R, COLOR_G, COLOR_B);
		for (Tuple<BlockPos, AxisAlignedBB> box : this.unsafeBox) {
			GlStateManager.glBegin(GL.GL_LINE_STRIP);
			this.renderBoxOutline(box.getFirst(), box.getSecond());
			GlStateManager.glEnd();
		}
		GL.glColor3f(COLOR_NR, COLOR_NG, COLOR_NB);
		for (Tuple<BlockPos, AxisAlignedBB> box : this.unsafeBoxNight) {
			GlStateManager.glBegin(GL.GL_LINE_STRIP);
			this.renderBoxOutline(box.getFirst(), box.getSecond());
			GlStateManager.glEnd();
		}
		
		GL.glEnableLighting();
		GL.glEnableTexture2D();
		GL.glDisableBlend();
		GL.glPopMatrix();
	}
	
	public void renderTop(BlockPos pos, AxisAlignedBB bb) {
		int x = pos.getX(), z = pos.getZ();
		double y = pos.getY() + LiteModSpawnHighlight.OFFSET + bb.maxY;
		GL11.glVertex3d(x + bb.maxX, y, z + bb.maxZ);
		GL11.glVertex3d(x + bb.minX, y, z + bb.minZ);
		GL11.glVertex3d(x + bb.maxX, y, z + bb.minZ);
		GL11.glVertex3d(x + bb.minX, y, z + bb.maxZ);
	}
	
	public void renderBoxOutline(BlockPos pos, AxisAlignedBB bb) {
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		bb = bb.expandXyz(LiteModSpawnHighlight.OFFSET);
		GL11.glVertex3d(x + bb.minX, y + bb.minY, z + bb.maxZ); //0
		GL11.glVertex3d(x + bb.maxX, y + bb.minY, z + bb.maxZ); //1
		GL11.glVertex3d(x + bb.maxX, y + bb.maxY, z + bb.maxZ); //3
		GL11.glVertex3d(x + bb.maxX, y + bb.minY, z + bb.maxZ); //1
		GL11.glVertex3d(x + bb.maxX, y + bb.minY, z + bb.minZ); //5
		GL11.glVertex3d(x + bb.minX, y + bb.minY, z + bb.minZ); //4
		GL11.glVertex3d(x + bb.minX, y + bb.maxY, z + bb.minZ); //6
		GL11.glVertex3d(x + bb.minX, y + bb.minY, z + bb.minZ); //4
		GL11.glVertex3d(x + bb.minX, y + bb.minY, z + bb.maxZ); //0
		GL11.glVertex3d(x + bb.minX, y + bb.maxY, z + bb.maxZ); //2
		GL11.glVertex3d(x + bb.maxX, y + bb.maxY, z + bb.maxZ); //3
		GL11.glVertex3d(x + bb.maxX, y + bb.maxY, z + bb.minZ); //7
		GL11.glVertex3d(x + bb.maxX, y + bb.minY, z + bb.minZ); //5
		GL11.glVertex3d(x + bb.maxX, y + bb.maxY, z + bb.minZ); //7
		GL11.glVertex3d(x + bb.minX, y + bb.maxY, z + bb.minZ); //6
		GL11.glVertex3d(x + bb.minX, y + bb.maxY, z + bb.maxZ); //2
	}
	
	public void renderBox(BlockPos pos, AxisAlignedBB bb) {
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		bb = bb.expandXyz(LiteModSpawnHighlight.OFFSET);
		GL11.glVertex3d(x + bb.maxX, y + bb.maxY, z + bb.minZ); //7
		GL11.glVertex3d(x + bb.minX, y + bb.maxY, z + bb.minZ); //6
		GL11.glVertex3d(x + bb.maxX, y + bb.maxY, z + bb.maxZ); //3
		GL11.glVertex3d(x + bb.minX, y + bb.maxY, z + bb.maxZ); //2
		GL11.glVertex3d(x + bb.minX, y + bb.minY, z + bb.maxZ); //0
		GL11.glVertex3d(x + bb.minX, y + bb.maxY, z + bb.minZ); //6
		GL11.glVertex3d(x + bb.minX, y + bb.minY, z + bb.minZ); //4
		GL11.glVertex3d(x + bb.maxX, y + bb.maxY, z + bb.minZ); //7
		GL11.glVertex3d(x + bb.maxX, y + bb.minY, z + bb.minZ); //5
		GL11.glVertex3d(x + bb.maxX, y + bb.maxY, z + bb.maxZ); //3
		GL11.glVertex3d(x + bb.maxX, y + bb.minY, z + bb.maxZ); //1
		GL11.glVertex3d(x + bb.minX, y + bb.minY, z + bb.maxZ); //0
		GL11.glVertex3d(x + bb.maxX, y + bb.minY, z + bb.minZ); //5
		GL11.glVertex3d(x + bb.minX, y + bb.minY, z + bb.minZ); //4
	}
}
