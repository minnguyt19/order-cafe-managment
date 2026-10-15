package model;

import java.util.ArrayList;
import java.util.Date;

public class Order implements Identifiable {
    private String id;
    private ArrayList<OrderDetail> listOrderDetails;
    private Date createdAt;
    private Status status;

    public Order() {
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public String setId() {
        return "";
    }

    public ArrayList<OrderDetail> getListOrderDetails() {
        return listOrderDetails;
    }

    public void setListOrderDetails(ArrayList<OrderDetail> listOrderDetails) {
        this.listOrderDetails = listOrderDetails;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }




}
