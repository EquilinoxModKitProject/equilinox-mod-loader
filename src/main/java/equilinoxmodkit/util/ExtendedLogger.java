package equilinoxmodkit.util;


import equilinoxmodkit.loader.LaunchHelper;


/* Log writer that used the log writer included in the Equilinox Mod Kit to only print messages if
 * debug mode is enabled using the '-EmlDebug' launch argument. */
public class ExtendedLogger {
	
	
	public static void log( Object... msg ) {
		if(LaunchHelper.isEmlDebugModeEnabled()) EmkLogger.logAdv( Thread.currentThread().getStackTrace()[ 2 ].getClassName(),msg );
	}
	
	public static void warn( Object... msg ) {
		if(LaunchHelper.isEmlDebugModeEnabled()) EmkLogger.warnAdv( Thread.currentThread().getStackTrace()[ 2 ].getClassName(),msg );
	}
	
	
}
