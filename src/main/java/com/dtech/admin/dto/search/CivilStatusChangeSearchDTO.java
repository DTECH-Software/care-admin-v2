package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class CivilStatusChangeSearchDTO {
    @SerializedName(value = "epfNo", alternate = {"epf", "epfNumber"})
    @JsonAlias({"epf", "epfNumber"})
    private String epfNo;
    private String staffCategory;
    private String company;
    private String civilStatus;
    private String status;
}
