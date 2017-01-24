package org.eclipse.paho.android.sample.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import org.eclipse.paho.android.sample.R;
import org.eclipse.paho.android.sample.internal.Connections;
import org.eclipse.paho.android.sample.model.ConnectionModel;

import java.util.Map;
import java.util.Random;


public class EditConnectionFragment extends Fragment {

    private EditText clientId;
    private EditText serverHostname;
    private EditText serverPort;
    private Switch cleanSession;
    private EditText username;
    private EditText password;
    private EditText tlsServerKey;
    private EditText tlsClientKey;
    private EditText timeout;
    private EditText keepAlive;
    private EditText lwtTopic;
    private EditText lwtMessage;
    private Spinner lwtQos;
    private Switch lwtRetain;

    private ConnectionModel formModel;
    private boolean newConnection = true;

    private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Random random = new Random();
    private static final int length = 8;

    public EditConnectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_edit_connection, container, false);
        clientId = (EditText) rootView.findViewById(R.id.client_id);
        serverHostname = (EditText) rootView.findViewById(R.id.hostname);
        serverPort = (EditText) rootView.findViewById(R.id.add_connection_port);
        serverPort.setText("");
        cleanSession = (Switch) rootView.findViewById(R.id.clean_session_switch);
        username = (EditText) rootView.findViewById(R.id.username);
        password = (EditText) rootView.findViewById(R.id.password);
        tlsServerKey = (EditText) rootView.findViewById(R.id.tls_server_key);
        tlsClientKey = (EditText) rootView.findViewById(R.id.tls_client_key);
        timeout = (EditText) rootView.findViewById(R.id.timeout);
        keepAlive = (EditText) rootView.findViewById(R.id.keepalive);
        lwtTopic = (EditText) rootView.findViewById(R.id.lwt_topic);
        lwtMessage = (EditText) rootView.findViewById(R.id.lwt_message);
        lwtQos = (Spinner) rootView.findViewById(R.id.lwt_qos_spinner);
        lwtRetain = (Switch) rootView.findViewById(R.id.retain_switch);



        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.qos_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lwtQos.setAdapter(adapter);






        if(this.getArguments() != null && this.getArguments().getString(ActivityConstants.CONNECTION_KEY) != null){
            /** This Form is referencing an existing connection. **/
            //this.getArguments().getString(ActivityConstants.CONNECTION_KEY)
            Map<String, Connection> connections =  Connections.getInstance(this.getActivity())
                    .getConnections();
            String connectionKey = this.getArguments().getString(ActivityConstants.CONNECTION_KEY);
            Connection connection = connections.get(connectionKey);
            System.out.println("Editing an existing connection: " + connection.handle());
            newConnection = false;
            formModel = new ConnectionModel(connection);
            System.out.println("Form Model: " + formModel.toString());
            formModel.setClientHandle(connection.handle());

            populateFromConnectionModel(formModel);

        } else {
            formModel = new ConnectionModel();
            populateFromConnectionModel(formModel);

        }

        setFormItemListeners();


        // Inflate the layout for this fragment
        return rootView;
    }

    private void setFormItemListeners(){
       clientId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                formModel.setClientId(s.toString());
            }
        });

        serverHostname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                formModel.setServerHostName(s.toString());
            }
        });

        serverPort.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() > 0) {
                    formModel.setServerPort(Integer.parseInt(s.toString()));
                }
            }
        });

        cleanSession.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                formModel.setCleanSession(isChecked);
            }
        });

        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().trim().equals("")) {
                    formModel.setUsername(s.toString());
                } else {
                    formModel.setUsername(ActivityConstants.empty);
                }

            }
        });

        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().trim().equals("")) {
                    formModel.setPassword(s.toString());
                } else {
                    formModel.setPassword(ActivityConstants.empty);
                }
            }
        });
        tlsServerKey.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                formModel.setTlsServerKey(s.toString());
            }
        });
        tlsClientKey.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                formModel.setTlsClientKey(s.toString());
            }
        });
        timeout.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() > 0) {
                    formModel.setTimeout(Integer.parseInt(s.toString()));
                }
            }
        });
        keepAlive.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() > 0) {
                    formModel.setKeepAlive(Integer.parseInt(s.toString()));
                }
            }
        });
        lwtTopic.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                    formModel.setLwtTopic(s.toString());
            }
        });
        lwtMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                formModel.setLwtMessage(s.toString());
            }
        });
        lwtQos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                formModel.setLwtQos(Integer.parseInt(getResources().getStringArray(R.array.qos_options)[position]));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        lwtRetain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                formModel.setLwtRetain(isChecked);
            }
        });

    }

    @SuppressLint("SetTextI18n")
    private void populateFromConnectionModel(ConnectionModel connectionModel) {
        clientId.setText(connectionModel.getClientId());
        serverHostname.setText(connectionModel.getServerHostName());
       serverPort.setText(Integer.toString(connectionModel.getServerPort()));
        cleanSession.setChecked(connectionModel.isCleanSession());
        username.setText(connectionModel.getUsername());
        password.setText(connectionModel.getPassword());
        tlsServerKey.setText(connectionModel.getTlsServerKey());
        tlsClientKey.setText(connectionModel.getTlsClientKey());
        timeout.setText(Integer.toString(connectionModel.getTimeout()));
        keepAlive.setText(Integer.toString(connectionModel.getKeepAlive()));
        lwtTopic.setText(connectionModel.getLwtTopic());
        lwtMessage.setText(connectionModel.getLwtMessage());
        lwtQos.setSelection(connectionModel.getLwtQos());
        lwtRetain.setChecked(connectionModel.isLwtRetain());
    }

    private void saveConnection(){
        System.out.println("SAVING CONNECTION");
        System.out.println(formModel.toString());
        if(newConnection){
            // Generate a new Client Handle
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++){
                sb.append(AB.charAt(random.nextInt(AB.length())));
            }
            String clientHandle = sb.toString() + '-' + formModel.getServerHostName() + '-' + formModel.getClientId();
            formModel.setClientHandle(clientHandle);
            ((MainActivity) getActivity()).persistAndConnect(formModel);

        } else {
            // Update an existing connection

            ((MainActivity) getActivity()).updateAndConnect(formModel);
        }


    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_edit_connection, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save_connection) {
            saveConnection();
        }

        return super.onOptionsItemSelected(item);
    }

}