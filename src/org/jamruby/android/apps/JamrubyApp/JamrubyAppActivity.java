package org.jamruby.android.apps.JamrubyApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import crimsonwoods.android.apps.JamrubyApp.R;
import org.jamruby.core.Jamruby;
import org.jamruby.mruby.MRuby;
import org.jamruby.mruby.ParserState;
import org.jamruby.mruby.Value;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class JamrubyAppActivity extends Activity {
	private static final String STATE_EXTRA_SCRIPTS = "scripts";
	private static final String STATE_EXTRA_RESULT = "result";
	private Thread stdout;
	private Thread stderr;
	private Handler handler;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        handler = new Handler();
        
        final EditText etScript = findView(R.id.edittext_ruby_scripts);
        final Button btnRun = findView(R.id.button_run);
        final TextView tvResult = findView(R.id.textview_result);
        
        btnRun.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Jamruby jamruby = new Jamruby();
		        try {
		        	final ParserState p = jamruby.parse(etScript.getText().toString());
		        	final Value ret = jamruby.run(p);
		        	tvResult.setText(ret.toString(jamruby.state()));
		        } catch (Throwable t) {
		        	handleException(t, true);
		        } finally {
		        	jamruby.close();
		        }
			}
		});
    }
    
    @Override
    protected void onPause() {
    	try {
    		super.onPause();
    	} finally {
	    	if (null != stdout) {
	    		stdout.interrupt();
	    		stdout = null;
	    	}
	    	if (null != stderr) {
	    		stderr.interrupt();
	    		stderr = null;
	    	}
    	}
    }
    
    @Override
    protected void onResume() {
    	try {
    		super.onResume();
    	} finally {
	    	try {
				stdout = new StdioThread(MRuby.stdout(), "stdout:");
				stderr = new StdioThread(MRuby.stderr(), "stderr:");
				stdout.start();
				stderr.start();
			} catch (IOException e) {
				Log.e(e, "Can not get stdio.");
			}
    	}
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	final EditText etScripts = findView(R.id.edittext_ruby_scripts);
    	final TextView tvResult = findView(R.id.textview_result);
    	if (null != etScripts) {
    		outState.putString(STATE_EXTRA_SCRIPTS, etScripts.getText().toString());
    	}
    	if (null != tvResult) {
    		outState.putString(STATE_EXTRA_RESULT, tvResult.getText().toString());
    	}
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	final EditText etScripts = findView(R.id.edittext_ruby_scripts);
    	final TextView tvResult = findView(R.id.textview_result);
    	if (null != etScripts) {
    		etScripts.setText(savedInstanceState.getString(STATE_EXTRA_SCRIPTS));
    	}
    	if (null != tvResult) {
    		tvResult.setText(savedInstanceState.getString(STATE_EXTRA_RESULT));
    	}
    }
    
    @SuppressWarnings("unchecked")
	private <T extends View> T findView(int id) {
    	return (T)findViewById(id);
    }
    
    private final class StdioThread extends Thread {
    	private final InputStream is;
    	private final String label;
    	
    	public StdioThread(InputStream is, String label) {
    		this.is = is;
    		this.label = label;
    		setName(String.format("%s - %s", getClass().getSimpleName(), label));
    	}
    	
    	@Override
    	public void run() {
    		try {
    			try {
    				InputStreamReader isr = new InputStreamReader(is);
    				BufferedReader br = new BufferedReader(isr);
    				try {
    					while (!Thread.interrupted()) {
    						final String line = br.readLine();
    						if (null == line) {
    							break;
    						}
    						Log.i("%s %s", label, line);
    					}
    				} finally {
    					try {
    						br.close();
    					} finally {
    						isr.close();
    					}
    				}
    			} finally {
    				if (null != is) {
    					is.close();
    				}
    			}
    		} catch (Throwable t) {
    			handleException(t, false);
    		} finally {
    		}
    	}
    }
    
    private void handleException(Throwable t, boolean isFinish) {
    	String msg = t.getMessage();
    	if (null == msg || 0 == msg.length()) {
    		final Throwable cause = t.getCause();
    		if (null != cause) {
    			msg = cause.getMessage();
    		}
    		if (null == msg || 0 == msg.length()) {
    			msg = t.getClass().getName();
    		}
    	}
    	Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    	Log.e(t, "Uncaught Exception");
    	if (isFinish) {
	    	handler.post(new Runnable() {
				@Override
				public void run() {
					finish();
				}
			});
    	}
    }
}