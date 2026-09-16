package com.cobbletabs.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuración del mod, guardada en config/cobbletabs.json.
 * Se crea con los valores por defecto la primera vez que se inicia el juego.
 */
public class CobbleTabsConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("cobbletabs.json");

	/**
	 * Lista de pestañas; edita este archivo para añadir, quitar o reordenar.
	 * Se reparten en los laterales de la GUI: la primera mitad a la izquierda,
	 * el resto a la derecha (con 6 pestañas: 3 a cada lado).
	 */
	public List<TabEntry> tabs = defaultTabs();

	/** Opciones del logo del inventario. */
	public LogoOptions logo = new LogoOptions();

	/** Muestra un botoncito para ocultar/mostrar las pestañas. */
	public boolean showToggleButton = true;

	/** Estado actual de visibilidad de las pestañas (se guarda al alternar). */
	public boolean tabsVisible = true;

	public static class LogoOptions {
		/** Mostrar el logo en el inventario del jugador. */
		public boolean enabled = true;
		/** Tamaño en "píxeles de GUI" (44 = discreto; se ajusta a la Escala de GUI). */
		public int size = 44;
		/** Opacidad del logo: 0-100 (50 = marca de agua sutil). */
		public int opacity = 50;
	}

	public static class TabEntry {
		/** Identificador interno (usado para la clave de traducción cobbletabs.tab.<id>). */
		public String id = "";
		/** Comando que se ejecuta al hacer clic, por ejemplo "/pc". */
		public String command = "/";
		/** Item mostrado como icono, por ejemplo "cobblemon:pc". */
		public String icon = "minecraft:paper";
		/** true = la pestaña se muestra, false = oculta. */
		public boolean enabled = true;
		/** Texto del tooltip (opcional). Si se deja vacío usa la traducción o el comando. */
		public String label = "";
		/** Color del nombre: nombre (gray, yellow, green, white, light_red, pink...) o hex #RRGGBB. Vacío = color por defecto. */
		public String color = "";
		/** Nombre en negrita. */
		public boolean bold = true;
	}

	public static CobbleTabsConfig load() {
		if (Files.exists(PATH)) {
			try {
				CobbleTabsConfig cfg = GSON.fromJson(Files.readString(PATH), CobbleTabsConfig.class);
				if (cfg != null) {
					if (cfg.tabs == null) {
						cfg.tabs = defaultTabs();
					}
					if (cfg.logo == null) {
						cfg.logo = new LogoOptions();
					}
					cfg.sanitize();
					return cfg;
				}
			} catch (Exception e) {
				CobbleTabsClient.LOGGER.error("[CobbleTabs] No se pudo leer {}: usando valores por defecto.", PATH.getFileName(), e);
			}
		}
		CobbleTabsConfig cfg = new CobbleTabsConfig();
		cfg.sanitize();
		save(cfg);
		return cfg;
	}

	public static void save(CobbleTabsConfig cfg) {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(cfg));
		} catch (IOException e) {
			CobbleTabsClient.LOGGER.error("[CobbleTabs] No se pudo guardar la configuración.", e);
		}
	}

	/** Normaliza los datos: ignora entradas inválidas y añade el "/" al comando si falta. */
	private void sanitize() {
		if (logo.size < 16) {
			logo.size = 16;
		} else if (logo.size > 256) {
			logo.size = 256;
		}
		if (logo.opacity < 0) {
			logo.opacity = 0;
		} else if (logo.opacity > 100) {
			logo.opacity = 100;
		}
		List<TabEntry> clean = new ArrayList<>();
		for (TabEntry t : tabs) {
			if (t == null || t.id == null || t.id.isBlank() || t.command == null || t.command.isBlank()) {
				continue;
			}
			t.id = t.id.trim();
			t.command = t.command.trim();
			if (!t.command.startsWith("/")) {
				t.command = "/" + t.command;
			}
			if (t.icon == null || t.icon.isBlank()) {
				t.icon = "minecraft:paper";
			}
			if (t.label == null) {
				t.label = "";
			}
			clean.add(t);
		}
		tabs = clean;
	}

	private static List<TabEntry> defaultTabs() {
		List<TabEntry> list = new ArrayList<>();
		list.add(entry("pc", "/pc", "cobblemon:pc", "PC", "gray"));
		list.add(entry("wiki", "/wiki", "cobblemon:pokedex_red", "Wiki", "light_red"));
		list.add(entry("daycare", "/daycare", "minecraft:book", "Daycare", "pink"));
		list.add(entry("daily", "/daily", "minecraft:clock", "Daily", "yellow"));
		list.add(entry("sts", "/sts", "cobblemon:verdant_ball", "STS", "white"));
		list.add(entry("wt", "/wt", "cobblemon:premier_ball", "WT", "green"));
		return list;
	}

	private static TabEntry entry(String id, String command, String icon, String label, String color) {
		TabEntry e = new TabEntry();
		e.id = id;
		e.command = command;
		e.icon = icon;
		e.label = label;
		e.color = color;
		e.bold = true;
		return e;
	}

	/** Colores por defecto de las pestañas clásicas, usado para migrar configs antiguas. */
	public static int defaultColorFor(String id) {
		if (id == null) {
			return 0xFFFFFF;
		}
		return switch (id) {
			case "pc" -> 0xAAAAAA;      // gris
			case "wiki" -> 0xFF5555;    // rojo claro
			case "daycare" -> 0xFF9FDB; // rosa
			case "daily" -> 0xFFFF55;   // amarillo
			case "sts" -> 0xFFFFFF;     // blanco
			case "wt" -> 0x55FF55;      // verde
			default -> 0xFFFFFF;
		};
	}

	/**
	 * Convierte un color de la config a RGB. Acepta nombres en inglés y español
	 * (gray/gris, yellow/amarillo, green/verde, white/blanco, red/rojo,
	 * light_red/rojo_claro, pink/rosa) o hex "#RRGGBB". Devuelve fallback si es
	 * inválido (fallback -1 = sin color personalizado).
	 */
	public static int parseColor(String s, int fallback) {
		if (s == null || s.isBlank()) {
			return fallback;
		}
		try {
			String c = s.trim().toLowerCase();
			return switch (c) {
				case "gray", "gris" -> 0xAAAAAA;
				case "yellow", "amarillo" -> 0xFFFF55;
				case "green", "verde" -> 0x55FF55;
				case "white", "blanco" -> 0xFFFFFF;
				case "light_red", "rojo_claro", "rojo claro", "red", "rojo" -> 0xFF5555;
				case "pink", "rosa" -> 0xFF9FDB;
				default -> c.startsWith("#") ? Integer.parseInt(c.substring(1), 16) : fallback;
			};
		} catch (Exception e) {
			return fallback;
		}
	}
}
