package com.cobbletabs.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CobbleTabsClient implements ClientModInitializer {
	public static final String MOD_ID = "cobbletabs";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Tamaño de cada pestaña, similar a las pestañas del inventario creativo. */
	private static final int TAB_WIDTH = 26;
	private static final int TAB_HEIGHT = 30;
	/** Solape de la pestaña sobre el borde del fondo de la GUI. */
	private static final int OVERLAP = 2;
	/** Espacio entre pestañas apiladas verticalmente en los laterales. */
	private static final int TAB_GAP = 2;
	/** Separación desde el borde izquierdo del fondo de la GUI. */
	private static final int MARGIN = 3;

	/** Tecla para recargar la configuración sin reiniciar (por defecto F8). */
	private static KeyMapping reloadKey;

	/** Logo del mod, dibujado en la esquina superior derecha del inventario. */
	private static final ResourceLocation LOGO_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/logo.png");

	private static CobbleTabsConfig config;
	private static List<Tab> tabs = List.of();

	private record Tab(String command, Component label, String iconId) {
	}

	@Override
	public void onInitializeClient() {
		config = CobbleTabsConfig.load();
		buildTabs();

		reloadKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.cobbletabs.reload",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_F8,
				"key.categories.cobbletabs"
		));

		// F8 en el juego, sin pantallas abiertas
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.screen == null) {
				while (reloadKey.consumeClick()) {
					reloadConfig(client);
				}
			}
		});

		ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			// En cofres y enderchests (ContainerScreen) no se muestran las pestañas
			if (screen instanceof AbstractContainerScreen<?> container && !(screen instanceof ContainerScreen)) {
				// En cofres y enderchests (ChestScreen) no se muestran las pestañas
				// F8 con una pantalla de contenedor abierta (inventario, cofres...)
				ScreenKeyboardEvents.allowKeyPress(screen).register((s, keyCode, scanCode, modifiers) -> {
					if (reloadKey.matches(keyCode, scanCode)) {
						reloadConfig(client);
						return false;
					}
					return true;
				});

				ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, tickDelta) -> {
					renderTabs(graphics, container, mouseX, mouseY);
					// El logo solo se muestra en el inventario del jugador (no en cofres ni enderchests)
					if (screen instanceof InventoryScreen) {
						renderLogo(graphics, container);
					}
				});

				ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) -> {
					if (button == 0 || button == 1) {
						// Botón de mostrar/ocultar pestañas
						if (toggleAt(container, mouseX, mouseY)) {
							config.tabsVisible = !config.tabsVisible;
							CobbleTabsConfig.save(config);
							return false;
						}
						if (config.tabsVisible) {
							Tab clicked = tabAt(container, mouseX, mouseY);
							if (clicked != null) {
								runCommand(clicked);
								s.onClose();
								return false; // cancela el procesamiento vanilla del clic
							}
						}
					}
					return true;
				});
			}
		});
		LOGGER.info("[CobbleTabs] Inicializado: {} pestañas activas (config: {}).", tabs.size(), config.tabs.size());
	}

	/** Construye la lista de pestañas visibles a partir de la configuración. */
	private static void buildTabs() {
		List<Tab> list = new ArrayList<>();
		for (CobbleTabsConfig.TabEntry entry : config.tabs) {
			if (!entry.enabled) {
				continue;
			}
			MutableComponent base = entry.label.isBlank()
					? Component.translatable("cobbletabs.tab." + entry.id)
					: Component.literal(entry.label);
			// Estilo desde la config: negrita y color (nombre o hex).
			// Si la pestaña es una de las de por defecto y no tiene color, se usa su color clásico
			// (así las configs antiguas se migran solas sin tocar nada).
			int rgb = CobbleTabsConfig.parseColor(entry.color, CobbleTabsConfig.defaultColorFor(entry.id));
			boolean bold = entry.bold;
			Component label = base.withStyle(style -> style.withBold(bold).withColor(TextColor.fromRgb(rgb)));
			list.add(new Tab(entry.command, label, entry.icon));
		}
		tabs = List.copyOf(list);
	}

	/** Resuelve el item del icono; fallback a papel si el id no existe o Cobblemon no está. */
	private static ItemStack iconStack(Tab tab) {
		try {
			ResourceLocation id = ResourceLocation.parse(tab.iconId());
			Item item = BuiltInRegistries.ITEM.get(id);
			if (item != Items.AIR) {
				return new ItemStack(item);
			}
		} catch (Exception ignored) {
			// id con formato inválido
		}
		return new ItemStack(Items.PAPER);
	}

	// ==================================================================
	// Renderizado
	// ==================================================================

	/** Pestañas en los laterales de la GUI: la mitad izquierda a la izquierda, el resto a la derecha. */
	private static void renderTabs(GuiGraphics graphics, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
		Minecraft client = Minecraft.getInstance();
		renderToggle(graphics, client, screen, mouseX, mouseY);
		if (!config.tabsVisible) {
			return;
		}
		int leftCount = (tabs.size() + 1) / 2;

		// Lado izquierdo
		int y = screen.topPos + MARGIN;
		for (int i = 0; i < leftCount; i++) {
			renderTab(graphics, client, tabs.get(i), screen.leftPos - TAB_WIDTH + OVERLAP, y, mouseX, mouseY);
			y += TAB_HEIGHT + TAB_GAP;
		}

		// Lado derecho
		y = screen.topPos + MARGIN;
		for (int i = leftCount; i < tabs.size(); i++) {
			renderTab(graphics, client, tabs.get(i), screen.leftPos + screen.imageWidth - OVERLAP, y, mouseX, mouseY);
			y += TAB_HEIGHT + TAB_GAP;
		}
	}

	/**
	 * Dibuja el logo en la esquina superior derecha del inventario.
	 * El tamaño en pantalla se ajusta a la Escala de GUI de Minecraft para que
	 * ocupe siempre el mismo espacio relativo (por defecto 54 "píxeles de GUI",
	 * configurable en config/cobbletabs.json).
	 */
	private static void renderLogo(GuiGraphics graphics, AbstractContainerScreen<?> screen) {
		int texSize = config.logo.size;
		int guiLogoSize = scaleGui(texSize);
		int logoX = screen.leftPos + screen.imageWidth - guiLogoSize - MARGIN;
		// Dentro del fondo del inventario; si la GUI fuese muy pequeña, se recorta hacia dentro.
		int logoY = Math.max(screen.topPos + MARGIN, 2);

		// Opacidad tipo marca de agua para que no moleste visualmente
		float alpha = config.logo.opacity / 100.0F;
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
		graphics.blit(LOGO_TEXTURE, logoX, logoY, guiLogoSize, guiLogoSize, 0.0F, 0.0F, texSize, texSize, texSize, texSize);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	/**
	 * Convierte "píxeles de GUI" a píxeles reales en pantalla según la Escala de GUI
	 * configurada en Minecraft, de modo que el tamaño relativo no cambie entre escalas.
	 */
	private static int scaleGui(int guiPixels) {
		double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		return Math.max(1, (int) Math.round(guiPixels * guiScale));
	}

	private static void renderTab(GuiGraphics graphics, Minecraft client, Tab tab, int x, int y, int mouseX, int mouseY) {
		boolean hovered = mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + TAB_HEIGHT;

		// Fondo y borde estilo pestaña
		graphics.fill(x, y, x + TAB_WIDTH, y + TAB_HEIGHT, hovered ? 0xE032323C : 0xD01C1C24);
		int border = hovered ? 0xFFF0F0F0 : 0xFF4A4A55;
		graphics.fill(x, y, x + TAB_WIDTH, y + 1, border);
		graphics.fill(x, y, x + 1, y + TAB_HEIGHT, border);
		graphics.fill(x + TAB_WIDTH - 1, y, x + TAB_WIDTH, y + TAB_HEIGHT, border);
		graphics.fill(x, y + TAB_HEIGHT - 1, x + TAB_WIDTH, y + TAB_HEIGHT, 0xFF000000);

		// Icono del item centrado
		graphics.renderItem(iconStack(tab), x + (TAB_WIDTH - 16) / 2, y + (TAB_HEIGHT - 16) / 2);

		if (hovered) {
			graphics.renderTooltip(client.font, tab.label(), mouseX, mouseY);
		}
	}

	// ==================================================================
	// Clic y ejecución de comandos
	// ==================================================================

	// ==================================================================
	// Botón para mostrar/ocultar las pestañas
	// ==================================================================

	private static final int TOGGLE_SIZE = 18;

	/** Dibuja el botón de mostrar/ocultar encima de la esquina superior derecha de la GUI. */
	private static void renderToggle(GuiGraphics graphics, Minecraft client, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
		int x = toggleX(screen);
		int y = toggleY(screen);
		boolean hovered = mouseX >= x && mouseX < x + TOGGLE_SIZE && mouseY >= y && mouseY < y + TOGGLE_SIZE;

		graphics.fill(x, y, x + TOGGLE_SIZE, y + TOGGLE_SIZE, hovered ? 0xE032323C : 0xD01C1C24);
		int border = hovered ? 0xFFF0F0F0 : 0xFF4A4A55;
		graphics.fill(x, y, x + TOGGLE_SIZE, y + 1, border);
		graphics.fill(x, y, x + 1, y + TOGGLE_SIZE, border);
		graphics.fill(x + TOGGLE_SIZE - 1, y, x + TOGGLE_SIZE, y + TOGGLE_SIZE, border);
		graphics.fill(x, y + TOGGLE_SIZE - 1, x + TOGGLE_SIZE, y + TOGGLE_SIZE, 0xFF000000);

		// Icono: barrera cuando se ven las pestañas (clic = ocultar), ojo de ender cuando están ocultas
		String iconId = config.tabsVisible ? "minecraft:barrier" : "minecraft:ender_eye";
		ItemStack icon = iconStack(new Tab("", Component.empty(), iconId));
		graphics.renderItem(icon, x + 1, y + 1);

		if (hovered) {
			Component tip = Component.translatable(config.tabsVisible ? "cobbletabs.toggle.hide" : "cobbletabs.toggle.show");
			graphics.renderTooltip(client.font, tip, mouseX, mouseY);
		}
	}

	private static int toggleX(AbstractContainerScreen<?> screen) {
		return screen.leftPos + screen.imageWidth - TOGGLE_SIZE;
	}

	private static int toggleY(AbstractContainerScreen<?> screen) {
		return Math.max(screen.topPos - TOGGLE_SIZE - 1, 2);
	}

	private static boolean toggleAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
		int x = toggleX(screen);
		int y = toggleY(screen);
		return mouseX >= x && mouseX < x + TOGGLE_SIZE && mouseY >= y && mouseY < y + TOGGLE_SIZE;
	}

	private static Tab tabAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
		if (!config.tabsVisible) {
			return null;
		}
		int leftCount = (tabs.size() + 1) / 2;

		// Lado izquierdo
		int y = screen.topPos + MARGIN;
		for (int i = 0; i < leftCount; i++) {
			int x = screen.leftPos - TAB_WIDTH + OVERLAP;
			if (mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + TAB_HEIGHT) {
				return tabs.get(i);
			}
			y += TAB_HEIGHT + TAB_GAP;
		}

		// Lado derecho
		y = screen.topPos + MARGIN;
		for (int i = leftCount; i < tabs.size(); i++) {
			int x = screen.leftPos + screen.imageWidth - OVERLAP;
			if (mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + TAB_HEIGHT) {
				return tabs.get(i);
			}
			y += TAB_HEIGHT + TAB_GAP;
		}
		return null;
	}

	/** Recarga config/cobbletabs.json y reconstruye las pestañas. */
	private static void reloadConfig(Minecraft client) {
		config = CobbleTabsConfig.load();
		buildTabs();
		if (client.player != null) {
			client.player.displayClientMessage(
					Component.literal("§a[CobbleTabs]§r Configuración recargada (" + tabs.size() + " pestañas)."),
					true);
		}
		LOGGER.info("[CobbleTabs] Configuración recargada: {} pestañas activas.", tabs.size());
	}

	private static void runCommand(Tab tab) {
		Minecraft client = Minecraft.getInstance();
		var handler = client.getConnection();
		if (handler == null) {
			return;
		}
		handler.sendCommand(tab.command().substring(1)); // sin el "/" inicial
	}
}
