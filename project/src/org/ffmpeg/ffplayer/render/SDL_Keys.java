package org.ffmpeg.ffplayer.render;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import org.ffmpeg.ffplayer.config.Globals;
import org.ffmpeg.ffplayer.render.SDLInput.SDL_1_2_Keycodes;
import org.ffmpeg.ffplayer.render.SDLInput.SDL_1_3_Keycodes;

import android.util.Log;

public class SDL_Keys
{
	private static final String LOG_TAG = "SDL_Keys";
	public static String [] names = null;
	public static Integer [] values = null;

	public static String [] namesSorted = null;
	public static Integer [] namesSortedIdx = null;
	public static Integer [] namesSortedBackIdx = null;
	
	public static final int JAVA_KEYCODE_LAST = 255; // Android 2.3 added several new gaming keys, Android 3.1 added even more - keep in sync with javakeycodes.h

	static
	{
		ArrayList<String> Names = new ArrayList<String> ();
		ArrayList<Integer> Values = new ArrayList<Integer> ();
		Field [] fields = SDL_1_2_Keycodes.class.getDeclaredFields();
		if(Globals.Using_SDL_1_3 )
		{
			fields = SDL_1_3_Keycodes.class.getDeclaredFields();
		}
		
		try {
			for(Field f: fields)
			{
				if (f.getType().getName().equals("int")) {
					Values.add(f.getInt(null));
					Names.add(f.getName().substring(5).toUpperCase());
				}
			}
		} catch(IllegalAccessException e) {};
		
		// Sort by value
		for( int i = 0; i < Values.size(); i++ )
		{
			for( int j = i; j < Values.size(); j++ )
			{
				if( Values.get(i) > Values.get(j) )
				{
					int x = Values.get(i);
					Values.set(i, Values.get(j));
					Values.set(j, x);
					String s = Names.get(i);
					Names.set(i, Names.get(j));
					Names.set(j, s);
				}
			}
		}
		
		names = Names.toArray(new String[0]);
		values = Values.toArray(new Integer[0]);
		namesSorted = Names.toArray(new String[0]);
		namesSortedIdx = new Integer[values.length];
		namesSortedBackIdx = new Integer[values.length];
		Arrays.sort(namesSorted);
		for( int i = 0; i < namesSorted.length; i++ )
		{
			for( int j = 0; j < namesSorted.length; j++ )
			{
				if( namesSorted[i].equals( names[j] ) )
				{
					namesSortedIdx[i] = j;
					namesSortedBackIdx[j] = i;
					break;
				}
			}
		}
	}
}