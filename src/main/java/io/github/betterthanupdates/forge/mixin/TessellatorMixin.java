package io.github.betterthanupdates.forge.mixin;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.Tessellator;

import io.github.betterthanupdates.forge.client.render.ForgeTessellator;

// FIXME
@Mixin(Tessellator.class)
public abstract class TessellatorMixin implements ForgeTessellator {
	@Shadow
	public static Tessellator INSTANCE;
	@Shadow
	private int vertexCount;
	@Shadow
	private boolean hasColor;
	@Shadow
	private boolean hasTexture;
	@Shadow
	private boolean hasNormals;
	@Shadow
	private int vertexAmount;
	@Shadow
	private boolean disableColor;
	@Shadow
	private int[] bufferArray;
	@Shadow
	private ByteBuffer byteBuffer;
	@Shadow
	private IntBuffer intBuffer;
	@Shadow
	private FloatBuffer floatBuffer;
	@Shadow
	private static boolean useTriangles;

	@Shadow
	protected abstract void clear();

	@Shadow
	private double textureX;
	@Shadow
	private double textureY;
	@Shadow
	private int color;
	@Shadow
	private int normal;
	@Shadow
	public double xOffset;
	@Shadow
	public double yOffset;
	@Shadow
	public double zOffset;
	@Shadow public boolean tessellating;
	@Shadow private int bufferIndex;
	@Shadow private int vboIndex;
	@Shadow private static boolean useVbo;
	@Shadow private boolean canUseVbo;
	@Shadow public int drawingMode;
	@Shadow private int vboCount;
	@Shadow private IntBuffer vertexBuffer;

	// Forge Fields
	@Unique
	public boolean defaultTexture = false;
	@Unique
	private int rawBufferSize;
	@Unique
	private static final int NATIVE_BUFFER_SIZE = 2097152;
	@Unique
	private static final int TRI_VERTS_IN_BUFFER = NATIVE_BUFFER_SIZE / 48 * 6;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void ctr$overwrite(int par1, CallbackInfo ci) {
		this.vertexCount = 0;
		this.hasColor = false;
		this.hasTexture = false;
		this.hasNormals = false;
		this.bufferIndex = 0;
		this.vertexAmount = 0;
		this.disableColor = false;
		this.tessellating = false;
		this.vboIndex = 0;
		this.bufferArray = null;
		this.rawBufferSize = 0;
	}

	@Inject(method = "<clinit>", at = @At("RETURN"))
	private static void classCtr$overwrite(CallbackInfo ci) {
		((TessellatorMixin) (Object) INSTANCE).defaultTexture = true;

		// TODO
	}

	/**
	 * @author Eloraam
	 * @reason Multi-tessellator
	 */
	@Overwrite
	public void tessellate() {
		if (!this.tessellating) {
			throw new IllegalStateException("Not tesselating!");
		} else {
			this.tessellating = false;
			int offs = 0;

			while (offs < this.vertexCount) {
				int vtc;

				if (this.drawingMode == 7 && useTriangles) {
					vtc = Math.min(this.vertexCount - offs, TRI_VERTS_IN_BUFFER);
				} else {
					vtc = Math.min(this.vertexCount - offs, NATIVE_BUFFER_SIZE >> 5);
				}

				intBuffer.clear();
				intBuffer.put(this.bufferArray, offs * 8, vtc * 8);
				byteBuffer.position(0);
				byteBuffer.limit(vtc * 32);
				offs += vtc;

				if (useVbo) {
					this.vboIndex = (this.vboIndex + 1) % vboCount;
					ARBVertexBufferObject.glBindBufferARB(34962, vertexBuffer.get(this.vboIndex));
					ARBVertexBufferObject.glBufferDataARB(34962, byteBuffer, 35040);
				}

				if (this.hasTexture) {
					if (canUseVbo) {
						GL11.glTexCoordPointer(2, 5126, 32, 12L);
					} else {
						floatBuffer.position(3);
						GL11.glTexCoordPointer(2, 32, floatBuffer);
					}

					GL11.glEnableClientState(32888);
				}

				if (this.hasColor) {
					if (canUseVbo) {
						GL11.glColorPointer(4, 5121, 32, 20L);
					} else {
						byteBuffer.position(20);
						GL11.glColorPointer(4, true, 32, byteBuffer);
					}

					GL11.glEnableClientState(32886);
				}

				if (this.hasNormals) {
					if (canUseVbo) {
						GL11.glNormalPointer(5120, 32, 24L);
					} else {
						byteBuffer.position(24);
						GL11.glNormalPointer(32, byteBuffer);
					}

					GL11.glEnableClientState(32885);
				}

				if (canUseVbo) {
					GL11.glVertexPointer(3, 5126, 32, 0L);
				} else {
					floatBuffer.position(0);
					GL11.glVertexPointer(3, 32, floatBuffer);
				}

				GL11.glEnableClientState(32884);

				if (this.drawingMode == 7 && useTriangles) {
					GL11.glDrawArrays(4, 0, vtc);
				} else {
					GL11.glDrawArrays(this.drawingMode, 0, vtc);
				}

				GL11.glDisableClientState(32884);

				if (this.hasTexture) {
					GL11.glDisableClientState(32888);
				}

				if (this.hasColor) {
					GL11.glDisableClientState(32886);
				}

				if (this.hasNormals) {
					GL11.glDisableClientState(32885);
				}
			}

			if (this.rawBufferSize > 131072 && this.bufferIndex < this.rawBufferSize << 3) {
				this.rawBufferSize = 0;
				this.bufferArray = null;
			}

			this.clear();
		}
	}

	/**
	 * @author Eloraam
	 * @reason method instruction order is different from vanilla.
	 */
	@Overwrite
	public void addVertex(double d, double d1, double d2) {
		if (this.bufferIndex >= this.rawBufferSize - 32) {
			if (this.rawBufferSize == 0) {
				this.rawBufferSize = 65536;
				this.bufferArray = new int[this.rawBufferSize];
			} else {
				this.rawBufferSize *= 2;
				this.bufferArray = Arrays.copyOf(this.bufferArray, this.rawBufferSize);
			}
		}

		++this.vertexAmount;

		if (this.drawingMode == 7 && useTriangles && this.vertexAmount % 4 == 0) {
			for (int i = 0; i < 2; ++i) {
				int j = 8 * (3 - i);

				if (this.hasTexture) {
					this.bufferArray[this.bufferIndex + 3] = this.bufferArray[this.bufferIndex - j + 3];
					this.bufferArray[this.bufferIndex + 4] = this.bufferArray[this.bufferIndex - j + 4];
				}

				if (this.hasColor) {
					this.bufferArray[this.bufferIndex + 5] = this.bufferArray[this.bufferIndex - j + 5];
				}

				this.bufferArray[this.bufferIndex + 0] = this.bufferArray[this.bufferIndex - j + 0];
				this.bufferArray[this.bufferIndex + 1] = this.bufferArray[this.bufferIndex - j + 1];
				this.bufferArray[this.bufferIndex + 2] = this.bufferArray[this.bufferIndex - j + 2];
				++this.vertexCount;
				this.bufferIndex += 8;
			}
		}

		if (this.hasTexture) {
			this.bufferArray[this.bufferIndex + 3] = Float.floatToRawIntBits((float) this.textureX);
			this.bufferArray[this.bufferIndex + 4] = Float.floatToRawIntBits((float) this.textureY);
		}

		if (this.hasColor) {
			this.bufferArray[this.bufferIndex + 5] = this.color;
		}

		if (this.hasNormals) {
			this.bufferArray[this.bufferIndex + 6] = this.normal;
		}

		this.bufferArray[this.bufferIndex + 0] = Float.floatToRawIntBits((float) (d + this.xOffset));
		this.bufferArray[this.bufferIndex + 1] = Float.floatToRawIntBits((float) (d1 + this.yOffset));
		this.bufferArray[this.bufferIndex + 2] = Float.floatToRawIntBits((float) (d2 + this.zOffset));
		this.bufferIndex += 8;
		++this.vertexCount;
	}

	@Override
	public boolean defaultTexture() {
		return this.defaultTexture;
	}

	@Override
	public void defaultTexture(boolean defaultTexture) {
		this.defaultTexture = defaultTexture;
	}

	@Override
	public boolean isTessellating() {
		return tessellating;
	}
}
