package fr.Alphart.BAT.Utils;


public class CallbackUtils {
	
	public interface Callback<T>{
		void done(final T result, final Throwable throwable);
	}
	
	public interface ProgressCallback<T> extends Callback<T>{
	    void onProgress(final T progressStatus);
	    
	    void onMinorError(final String errorMessage);
	}
	
	public static class VoidCallback implements Callback<Object>{

		@Override
		public final void done(final Object nullable, Throwable throwable) {
			
		}
		
	}
}
