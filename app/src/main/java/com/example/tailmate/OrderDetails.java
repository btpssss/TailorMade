package com.example.tailmate;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ernestoyaquello.com.verticalstepperform.Step;
import ernestoyaquello.com.verticalstepperform.VerticalStepperFormView;
import ernestoyaquello.com.verticalstepperform.listener.StepperFormListener;

public class OrderDetails extends AppCompatActivity implements StepperFormListener{

    private static final int CUSTOMER_UPDATED = 109;
    TextView orderName, orderId, CName, CNumber, CGender, date, totalAmt;
    ImageView back, menu, ahead;
    RelativeLayout customerDetails;
    RecyclerView recyclerView, recyclerView2;
    String Oid;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    FirebaseStorage firebaseStorage;
    private Steps received;
    private Steps userEmailStep;
    private Steps userAgeStep;

    public static VerticalStepperFormView verticalStepperForm;
    static Order order;
    static ItemListAdaptor itemlistadaptor;
    private AtWork inProgess;
    static ItemListAdaptor itemlistadaptor2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        orderName = findViewById(R.id.orderName);
        orderId = findViewById(R.id.orderId);
        CName = findViewById(R.id.customerName);
        CNumber = findViewById(R.id.customerPhone);
        CGender = findViewById(R.id.customerGender);
        back = findViewById(R.id.back);
        verticalStepperForm = findViewById(R.id.stepper_form);
        date = findViewById(R.id.DeliveryDate);
        recyclerView = findViewById(R.id.itemListView);
        totalAmt = findViewById(R.id.totalAmount);
        customerDetails = findViewById(R.id.CustomerLayout);
        menu = findViewById(R.id.menu);
        verticalStepperForm = findViewById(R.id.stepper_form);

        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        received = new Steps("Received");
        inProgess = new AtWork("Prepare Order");


        verticalStepperForm
                .setup((StepperFormListener) OrderDetails.this, received, inProgess)
                .basicColorScheme(getColor(R.color.default_dark), getColor(R.color.bg_grey), getColor(R.color.white))
                .displayBottomNavigation(false)
                .displayStepButtons(false)
                .allowStepOpeningOnHeaderClick(false)
                .init();

        recyclerView2 = verticalStepperForm.getStepContentLayout(1).findViewById(R.id.recyclerView2);
        ahead = verticalStepperForm.getStepContentLayout(1).findViewById(R.id.goAhead);
        recyclerView2.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        Intent in = getIntent();
        Oid = in.getStringExtra("Oid");
        fetchDetails();

        customerDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent in  = new Intent(OrderDetails.this, CustomerDetails.class);
                in.putExtra("Cid", order.getCustomer().getCid());
                in.putExtra("Number", order.getCustomer().getMobileNumber());
                in.putExtra("Name", order.getCustomer().getName());
                in.putExtra("Gender", order.getCustomer().getGender());
                startActivityForResult(in, CUSTOMER_UPDATED);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopupMenu(view);
            }
        });

    }


    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.inflate(R.menu.order_menu);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_edit:
                        Intent in = new Intent(OrderDetails.this, AddOrder.class);
                        in.putExtra("Activity", "Edit Order");
                        in.putExtra("Oid", order.getOrderID());
                        startActivityForResult(in, CUSTOMER_UPDATED);
                        return true;
                    case R.id.menu_print:
                        // Handle the Print option
                        return true;
                    case R.id.menu_expenses:
                        // Handle the Expenses option
                        return true;
                    default:
                        return false;
                }
            }
        });
        popupMenu.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK && data!=null)
        {
            if(requestCode==CUSTOMER_UPDATED)
            {
                fetchDetails();
            }
            else if(requestCode==456)
            {
                String totalCharges = data.getStringExtra("Total Charges");
                boolean isCompleted = data.getBooleanExtra("isCompleted", false);
                ArrayList<byte[]> dresses = (ArrayList<byte[]>) data.getSerializableExtra("Dresses");
                Gson gson = new Gson();
                String json = data.getStringExtra("Expenses");
                Type listType = new TypeToken<List<Pair<String, String>>>() {}.getType();
                List<kotlin.Pair<String, String>> expenses = gson.fromJson(json, listType);
                int pos = data.getIntExtra("Position", -1);
                String id = data.getStringExtra("Id");

                if(pos>=0)
                {
                    OrderItem orderItem = itemlistadaptor.getItem(pos);
                    orderItem.setExpenses(expenses);
                    orderItem.setDressImages(dresses);
                    orderItem.setTotalItemCharges(totalCharges);
                    orderItem.setComplete(isCompleted);
                    itemlistadaptor.updateItem(orderItem,pos);
                    itemlistadaptor2.updateItem(orderItem,pos);
                    totalAmt.setText("₹ " + itemlistadaptor.makeAmount());

                    Map<String,Object> m = new HashMap<>();
                    m.put("Item Name", orderItem.getName());
                    m.put("Item Type", orderItem.getType());
                    m.put("Charges", orderItem.getCharges());
                    m.put("Total amount", orderItem.getTotalItemCharges());
                    m.put("Expenses", orderItem.getExpenses());
                    m.put("isComplete", orderItem.isComplete());
                    m.put("Instructions",orderItem.getInstructions());
                    m.put("Body Measurements", orderItem.getBodyMs());
                    showLoadingDialog();
                    firebaseFirestore.collection("Orders").document(hash(firebaseAuth.getCurrentUser().getPhoneNumber()))
                            .collection("Details").document(Oid).collection("Item Details").document(id)
                            .set(m).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    if(dresses.size()>0)
                                        addDressImages(id,dresses);
                                    else
                                        dismissLoadingDialog();

                                    checkCanGoAhead();
                                }
                            });
                }
            }
        }
    }
    int k = 0;
    private void addDressImages(String id, ArrayList<byte[]> dresses) {

        k=0;

        for(byte[] arr: dresses)
        {
            firebaseStorage.getReference().child(hash(firebaseAuth.getCurrentUser().getPhoneNumber())).child("Customers")
                    .child(order.getCustomer().getCid()).child(order.getOrderID()).child(id).child("Dress " + String.valueOf(++k)).putBytes(arr)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            if(k== dresses.size())
                                dismissLoadingDialog();
                        }
                    });

        }
    }

    private void fetchDetails() {
        showLoadingDialog();
        DocumentReference dref = firebaseFirestore.collection("Orders").document(hash(firebaseAuth.getCurrentUser().getPhoneNumber()))
                .collection("Details").document(Oid);

        dref.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot ds) {
                        firebaseFirestore.collection("Customers").document(hash(firebaseAuth.getCurrentUser().getPhoneNumber()))
                                .collection("Details").document(ds.get("Cid").toString()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot d) {
                                        Customer customer = new Customer(d.get("Name").toString(), d.get("PhoneNumber").toString(),
                                                d.get("Gender").toString(), d.getId());

                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
                                        String status = ds.get("Status").toString();
                                        order = new Order(ds.getId(), ds.get("Order Name").toString(), status
                                                , LocalDate.parse(ds.get("Delivery Date").toString(), formatter), customer);
                                        order.setUrgent((Boolean) ds.get("Urgent"));
                                        order.setCharges(ds.get("Total Amount").toString());
                                        order.setDates((Map<String,String>)ds.get("Dates"));
                                        orderName.setText(order.getOrderName());
                                        orderId.setText("Order Id: "+order.getOrderID());
                                        CName.setText(customer.getName());
                                        CGender.setText(customer.getGender());
                                        CNumber.setText(customer.getMobileNumber());
                                        date.setText("Delivery Date: " + ds.get("Delivery Date").toString());
                                        totalAmt.setText(order.getCharges());
                                        fetchItems();
                                        goToRightStep(status);
                                    }
                                });

                    }
                });
    }

    private void goToRightStep(String status) {
        int stayAt = 0;
        if(status.equals("Upcoming"))
            stayAt = 0;
        else if(status.equals("Active"))
            stayAt = 1;
        else if(status.equals("Completed"))
            stayAt = 2;
        else if(status.equals("Delivered"))
            stayAt = 3;

        for(int i=0; i<stayAt; i++)
        {
            verticalStepperForm.markStepAsCompleted(i, false);
            verticalStepperForm.goToNextStep(false);
        }
    }

    private void fetchItems() {
        itemlistadaptor = new ItemListAdaptor(getApplicationContext(), OrderDetails.this, true);
        itemlistadaptor2 = new ItemListAdaptor(getApplicationContext(), OrderDetails.this, false);

        recyclerView.setAdapter(itemlistadaptor);
        recyclerView2.setAdapter(itemlistadaptor2);

        StorageReference sref = firebaseStorage.getReference().child(hash(firebaseAuth.getCurrentUser().getPhoneNumber())).child("Customers")
                .child(order.getCustomer().getCid()).child(order.getOrderID());

        firebaseFirestore.collection("Orders").document(hash(firebaseAuth.getCurrentUser().getPhoneNumber()))
                .collection("Details").document(Oid).collection("Item Details")
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                        for(DocumentSnapshot ds: queryDocumentSnapshots)
                        {
                            OrderItem orderItem = new OrderItem();
                            orderItem.setId(ds.getId());
                            orderItem.setName((String) ds.get("Item Name"));
                            orderItem.setType((String) ds.get("Item Type"));
                            orderItem.setInstructions((List<String>) ds.get("Instructions"));
                            orderItem.setCharges((String) ds.get("Charges"));
                            orderItem.setComplete((boolean) ds.get("isComplete"));
                            orderItem.setTotalItemCharges((String) ds.get("Total amount"));
                            if(ds.get("Expenses")!=null)
                                orderItem.setExpenses((List<kotlin.Pair<String, String>>) ds.get("Expenses"));
                            orderItem.setBodyMs((Map<String, String>) ds.get("Body Measurements"));

                            sref.child(ds.getId()).listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                                @Override
                                public void onSuccess(ListResult listResult) {
                                    ArrayList<byte[]> cloths = new ArrayList<>();
                                    ArrayList<byte[]> pattern = new ArrayList<>();
                                    ArrayList<byte[]> dress = new ArrayList<>();

                                    for(StorageReference item : listResult.getItems())
                                    {
                                        item.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                            @Override
                                            public void onSuccess(byte[] bytes) {
                                                String itemName = item.getName();

                                                if(itemName.contains("Cloth"))
                                                {
                                                    cloths.add(bytes);
                                                }
                                                else if(itemName.contains("Pattern"))
                                                {
                                                    pattern.add(bytes);
                                                }
                                                else
                                                {
                                                    dress.add(bytes);
                                                }
                                                if(cloths.size() + pattern.size() + dress.size() == listResult.getItems().size())
                                                {
                                                    orderItem.setPatternImages(pattern);
                                                    orderItem.setClothImages(cloths);
                                                    orderItem.setDressImages(dress);

                                                    itemlistadaptor.addItem(orderItem);
                                                    itemlistadaptor2.addItem(orderItem);
                                                    if(itemlistadaptor.getItemCount()==queryDocumentSnapshots.getDocuments().size())
                                                    {
                                                        totalAmt.setText("₹ " + itemlistadaptor.makeAmount());
                                                        checkCanGoAhead();
                                                        dismissLoadingDialog();
                                                    }
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                });
    }

    public void checkCanGoAhead()
    {
        int count = itemlistadaptor2.getItemCount();
        for(OrderItem orderItem: itemlistadaptor2.getOrderItems())
        {
            if(orderItem.isComplete())
                count--;
        }

        if(count!=0)
            ahead.setVisibility(View.GONE);
        else
            ahead.setVisibility(View.VISIBLE);
    }


    @Override
    public void onCompletedForm() {

    }

    @Override
    public void onCancelledForm() {

    }

    @Override
    public void onStepAdded(int index, Step<?> addedStep) {

    }

    @Override
    public void onStepRemoved(int index) {

    }

    private static String hash(String phoneNumber) {
        String hash = phoneNumber;

        try {
            // Create an instance of the MD5 hashing algorithm
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Convert the phone number to bytes and generate the hash
            md.update(phoneNumber.getBytes());
            byte[] digest = md.digest();

            // Convert the byte array to a BigInteger
            BigInteger bigInt = new BigInteger(1, digest);

            // Convert the BigInteger to a hexadecimal string
            hash = bigInt.toString(16);
        } catch (NoSuchAlgorithmException e) {
            // Handle exceptions related to the hashing algorithm
            e.printStackTrace();
        }

        return hash;
    }
    private ProgressDialog progressDialog;

    private void showLoadingDialog() {
        progressDialog = new ProgressDialog(OrderDetails.this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void dismissLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        setResult(RESULT_OK,intent);
        finish();
        super.onBackPressed();

    }
}