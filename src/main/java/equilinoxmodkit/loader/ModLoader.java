package equilinoxmodkit.loader;


import equilinoxmodkit.event.EmkEvent;
import equilinoxmodkit.mod.*;
import equilinoxmodkit.util.EmkLogger;
import equilinoxmodkit.util.ExtendedLogger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/* Loads and manages loaded mods. */
public class ModLoader {
	
	
	private static ArrayList<EquilinoxMod> loadedMods = new ArrayList<>();
	private static int numOfLoadedMods;
	private static int numOfRejectedMods;
	
	
	public static void initializeMods() {
		Initializer initializer = new Initializer();
		loadedMods.forEach( mod -> mod.init( initializer ) );
	}
	
	public static ArrayList<EquilinoxMod> getLoadedMods() {
		return loadedMods;
	}
	
	public static ArrayList<ModInfo> getLoadedModsModInfos() {
		ArrayList<ModInfo> modInfos = new ArrayList<>();
		loadedMods.forEach( mod -> modInfos.add( mod.getModInfo() ) );
		return modInfos;
	}
	
	public static ArrayList<Dependency> getLoadedModDependencies() {
		ArrayList<Dependency> dependencies = new ArrayList<>();
		loadedMods.forEach( mod -> dependencies.add( mod.getDependency() ) );
		return dependencies;
	}
	
	public static int getNumberOfLoadedMods() {
		return numOfLoadedMods;
	}
	
	public static int getNumberOfRejectedMods() {
		return numOfRejectedMods;
	}
	
	
	static void loadMods() {
		EmkLogger.log( "Loading mods from 'mods' folder" );
		try {
			URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			Method sysloader_addURL = URLClassLoader.class.getDeclaredMethod( "addURL",URL.class );
			sysloader_addURL.setAccessible( true );
			
			JarFile jarFile;
			String className;
			Class<?> clazz;
			EquilinoxMod mod;
			
			for( File file : LaunchHelper.getModsDir().listFiles() ) {
				if(file.getName().endsWith( ".jar" )) {
					ExtendedLogger.log( " - searching file " + file.getName() );
					sysloader_addURL.invoke( sysloader,file.toURI().toURL() );
					jarFile = new JarFile( file );
					for( JarEntry entry : Collections.list( jarFile.entries() ) ) {
						if(entry.getName().endsWith( ".class" )) {
							className = entry.getName().replace( ".class","" ).replace( "/","." );
							clazz = Class.forName( className );
							if(clazz.getSuperclass().equals( EquilinoxMod.class )) {
								ExtendedLogger.log( " - loading " + className );
								mod = ModLoader.createInstance( clazz );
								if(isModInfoValid( mod.getModInfo() )) {
									loadedMods.add( mod );
									numOfLoadedMods++;
								} else {
									ExtendedLogger.warn( className + " has an invalid ModInfo" );
									numOfRejectedMods++;
								}
							}
						}
					}
				}
			}
		} catch( IOException | IllegalAccessException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException e ) {
			e.printStackTrace();
		}
		ExtendedLogger.log( " - loaded ",numOfLoadedMods," mods" );
		ExtendedLogger.log( " - rejected ",numOfRejectedMods," mods" );
	}
	
	static void sortMods() {
		EmkLogger.log( "Sorting mods according to dependencies" );
		int numOfDependencies;
		HashMap<Integer,ArrayList<EquilinoxMod>> modBatches = new HashMap<>();
		for( EquilinoxMod mod : loadedMods ) {
			if(mod.getDependency() != null) {
				numOfDependencies = mod.getDependency().dependencyIDs().length;
				if(modBatches.containsKey( numOfDependencies )) {
					modBatches.get( numOfDependencies ).add( mod );
				} else {
					ArrayList<EquilinoxMod> newBatch = new ArrayList<>();
					newBatch.add( mod );
					modBatches.put( numOfDependencies,newBatch );
				}
			}
		}
		
		
	}
	
	static void preInitializeMods() {
		PreInitializer preInitializer = new PreInitializer(
				LaunchHelper.isEmlDebugModeEnabled(),
				LaunchHelper.isEquilinoxDebugModeEnabled(),
				LaunchHelper.getOperatingSystem(),
				LaunchHelper.getEquilinoxDir(),
				LaunchHelper.getEquilinoxJar(),
				LaunchHelper.getLogFile(),
				LaunchHelper.getNativesDir(),
				LaunchHelper.getModsDir(),
				loadedMods,
				numOfLoadedMods,
				numOfRejectedMods
		);
		loadedMods.forEach( mod -> mod.preInit( preInitializer ) );
		handleEventClasses( preInitializer.getEventClasses() );
		handleBlueprintClasses( preInitializer.getBlueprintClasses() );
	}
	
	
	private static boolean isModInfoValid( ModInfo modInfo ) {
		if(modInfo != null) return !modInfo.id().isEmpty() && modInfo.version().matches( "^\\d\\.\\d\\.\\d" );
		return false;
	}
	
	private static EquilinoxMod createInstance( Class<?> clazz ) {
		try {
			for( Constructor<?> constructor : clazz.getConstructors() ) {
				if(constructor.getParameterCount() == 0) {
					constructor.setAccessible( true );
					return (EquilinoxMod) constructor.newInstance();
				} else {
					ExtendedLogger.warn( clazz.getName()," has no zero-args constructor present" );
					numOfLoadedMods++;
				}
			}
		} catch( IllegalAccessException | InstantiationException | InvocationTargetException e ) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static void handleEventClasses( ArrayList<Class<?>> classes ) {
		for( Class<?> clazz : classes ) {
			for( Method method : clazz.getDeclaredMethods() ) {
				if(method.getParameterCount() == 1 && method.isAnnotationPresent( EmkEvent.class )) {
					EventManager.addMethod( method.getParameters()[ 0 ].getType(),method,clazz );
				}
			}
		}
	}
	
	private static void handleBlueprintClasses( ArrayList<Class<?>> classes ) {
	
	}
	
	
}
