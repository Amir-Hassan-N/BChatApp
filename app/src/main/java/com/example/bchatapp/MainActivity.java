package com.example.bchatapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.BreakIterator;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button listen,listDevices,send;
    ListView listView;
    TextView msg_box,status;

    EditText writeMsg;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] btArray;

    SendReceive sendReceive;

    static final int STATE_LISTENING=1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAIL=4;
    static final int STATE_MESSAGE_RECEIVED=5;

    int REQUEST_ENABLE_BLUETOOTH=1;
  //  ImageView imageView;

    private static final String APP_NAME="BChatApp";
    private static final UUID MY_UUID=UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // DataBase Class
        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "database-name").build();
        // Database END
        findViewByIdes();  // Method
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is Not Available ", Toast.LENGTH_LONG).show();
            finish(); // automatically finish if Bluetooth is not Available
        }
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableIntent=new Intent(bluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent,REQUEST_ENABLE_BLUETOOTH);
            }
    
         implementListeners();  // Method
    }

    private void implementListeners() {
        listDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();  // changed
                // here we want to show all the paired devices through this method
                Set<BluetoothDevice> bt=bluetoothAdapter.getBondedDevices();
                String[] strings=new String[bt.size()];
                btArray=new BluetoothDevice[bt.size()]; // new Line here
                int index=0;
                if(bt.size()>0)
                {
                    for (BluetoothDevice device:bt)
                    {  // here we are saving all the device
                        btArray[index]=device;
                        strings[index]=device.getName(); // getting the name of the devices
                        index++;
                    }
                    ArrayAdapter<String> arrayAdapter =new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,strings);
                    listView.setAdapter(arrayAdapter);
                }
            }
        });
        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ServerClass serverClass=new ServerClass(); // When we click on Listener object will be created
                serverClass.start();                      // and thread will start
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ClientClass clientClass=new ClientClass(btArray[i]);
                clientClass.start();  // it will start the thread
                status.setText("CONNECTING");
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String string= String.valueOf(writeMsg.getText());
                sendReceive.write(string.getBytes());
                writeMsg.setText(" ");

            }
        });
    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what)
            {
                case STATE_LISTENING:
                    status.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    status.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    status.setText("Connected");
                    break;
                case STATE_CONNECTION_FAIL:
                    status.setText("Connection Fail");
                    break;
                case STATE_MESSAGE_RECEIVED:
                   byte[] readBuff=(byte[])msg.obj;       // we will do later
                   String tempMsg=new String(readBuff,0,msg.arg1);   // problem can happen here arg2
                   msg_box.setText(msg_box.getText()+"\n"+tempMsg);       // chages Done // error can happen here **********************

                    break;
            }
            return true;
        }
    });

    private void findViewByIdes() {
        // here left name are mainactivity name and R.id.names are .xml file names
        listen=(Button) findViewById(R.id.listen);
        listDevices=(Button) findViewById(R.id.listDevices);
        send=(Button) findViewById(R.id.send);
        listView=(ListView) findViewById(R.id.listView);
        msg_box=(TextView) findViewById(R.id.msg);
        status=(TextView) findViewById(R.id.status);
        writeMsg=(EditText) findViewById(R.id.writeMsg);
    }

    private class ServerClass extends Thread
    {
        private BluetoothServerSocket serverSocket;

        public ServerClass() // Constructor
        {
            try {
                serverSocket=bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void run()
        {
            BluetoothSocket socket=null;
            while(socket==null){
                try {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket=serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTION_FAIL;
                    handler.sendMessage(message);

                }
                if(socket!=null)
                {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);
                    // Write Some code for Send and Receive **************************
                   sendReceive=new SendReceive(socket); // sendReceive Object
                   sendReceive.start();                // Thread is Starting from Here
                    break;
                }
            }
        }
    }

    private class ClientClass extends Thread
    {
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass(BluetoothDevice device)
        {
            this.device=device;   // this before was device1 right side device=device1
            try {
                socket=device.createRfcommSocketToServiceRecord(MY_UUID); // Error was here *****************
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void run(){
           // bluetoothAdapter.cancelDiscovery(); // add a new line here for cancel the Discovery
            try {
                socket.connect();       // when the device is connecting we will send the message to the handler.
                Message message=Message.obtain();
                message.what=STATE_CONNECTED;
                handler.sendMessage(message);

               sendReceive=new SendReceive(socket);
               sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message=Message.obtain();
                message.what=STATE_CONNECTION_FAIL;
                handler.sendMessage(message);
            }
        }
    }

    private class SendReceive extends Thread   // Now it's Complete
    {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive(BluetoothSocket socket)
        {
            this.bluetoothSocket=socket;
            InputStream tempIn=null;   // temporary Object
            OutputStream tempOut=null;
            try {
                tempIn=bluetoothSocket.getInputStream();
                tempOut=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream=tempIn;
            outputStream=tempOut;
        }
        public void run()
        {

            byte[] buffer=new byte[1024];
            int bytes;
            // keep listening to the input Stream
            while (true)
            {
                try {
                    // Read fron the inputStream and buffer contain all the Message
                    bytes=inputStream.read(buffer);
                    // Send the obtained Message to the UI Activity
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {

                    e.printStackTrace();
                }
            }
        }
        // Write to outputStream
        public void write(byte[] bytes)
        {
            try {


              //  String string=String.valueOf(editText.getText());
              //  outputStream.write(string.getBytes());
                outputStream.write(bytes);
                handler.obtainMessage(STATE_LISTENING,-1,-1,bytes).sendToTarget();

               // outputStream.flush(); //'''''''''''''''''''''
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public  void cancel() throws IOException {
            bluetoothSocket.close();

        }
    }
}
