/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkl_shop.pracownik.produkty;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import mkl_shop.alert.AlertMaker;
import mkl_shop.connection.DBConnection;
import mkl_shop.pracownik.FXMLPracownikController;
import mkl_shop.pracownik.modele.Produkt;

/**
 * FXML Controller class
 *
 * @author BlackHawk
 */
public class FXMLProduktyController implements Initializable {

    @FXML
    private TableView<Produkt> tableProdukty;
    @FXML
    private TableColumn<Produkt, Integer> columnIDProduktu;
    @FXML
    private TableColumn<Produkt, String> columnNazwa;
    @FXML
    private TableColumn<Produkt, Double> columnCena;
    @FXML
    private TableColumn<Produkt, Integer> columnIlosc;
    @FXML
    private TableColumn<Produkt, String> columnOpis;
    @FXML
    private JFXButton bWyjscie;
    @FXML
    private JFXTextField tfSzukaj;
    @FXML
    private MenuItem miZapotrzebowanie;
    @FXML
    private JFXButton bNowyProdukt;
    @FXML
    private StackPane spMain;
    @FXML
    private AnchorPane apMain;

    private ObservableList<Produkt> dataProdukt;
    private Produkt pr;
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        dataProdukt = FXCollections.observableArrayList();

        Connection conn = DBConnection.Connect();

        try {
            Statement ps = conn.createStatement();
            ResultSet rs = ps.executeQuery("SELECT produkty.id_produktu, produkty.nazwa_produktu, produkty.cena_produktu, produkty.opis_produktu, "
                    + "placowka_produkt.id_placowki, placowka_produkt.ilosc_produktow, placowka_produkt.id_produktu, placowka_produkt.id_placowka_produkt,"
                    + " placowka.id_placowki FROM produkty,placowka_produkt,placowka WHERE placowka.id_placowki = " + FXMLPracownikController.idPlacowki + " "
                    + "AND placowka.id_placowki = placowka_produkt.id_placowki AND placowka_produkt.id_produktu = produkty.id_produktu;");

            while (rs.next()) {
                dataProdukt.add(new Produkt(rs.getInt("produkty.id_produktu"), rs.getString("produkty.nazwa_produktu"), rs.getDouble("produkty.cena_produktu"), rs.getString("produkty.opis_produktu"), rs.getInt("placowka_produkt.ilosc_produktow")));
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (SQLException ex) {
            Logger.getLogger(FXMLProduktyController.class.getName()).log(Level.SEVERE, null, ex);
        }

        columnIDProduktu.setCellValueFactory(new PropertyValueFactory<>("id_produktu"));
        columnNazwa.setCellValueFactory(new PropertyValueFactory<>("nazwa_produktu"));
        columnIlosc.setCellValueFactory(new PropertyValueFactory<>("ilosc_produktow"));
        columnCena.setCellValueFactory(new PropertyValueFactory<>("cena_produktu"));
        columnOpis.setCellValueFactory(new PropertyValueFactory<>("opis_produktu"));

        tableProdukty.setItems(null);
        tableProdukty.setItems(dataProdukt);

        FilteredList<Produkt> filteredProdukt = new FilteredList<>(tableProdukty.getItems(), e -> true);
        tfSzukaj.setOnKeyReleased(e -> {
            tfSzukaj.textProperty().addListener((observableValue, oldValue, newValue) -> {
                filteredProdukt.setPredicate((Predicate<? super Produkt>) k -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lcFilter = newValue.toLowerCase();
                    if (k.getNazwa_produktu().toLowerCase().contains(lcFilter) || k.getId_produktu().toString().toLowerCase().contains(lcFilter)) {
                        return true;
                    }
                    return false;
                });
            });
            SortedList<Produkt> sortedProdukty = new SortedList<>(filteredProdukt);
            tableProdukty.setItems(sortedProdukty);
        });

    }

    @FXML
    private void zamknijOkno(ActionEvent event) {
        Stage stage = (Stage) bWyjscie.getScene().getWindow();
        stage.close();

    }

    /**
     * Wysłanie wiadomości do kierownika placówki, w której zgłaszana jest mała ilość danego produktu w sklepie.
     * @param event 
     */
    @FXML
    private void zglosZapotrzebowanie(ActionEvent event) {
        
        
        JFXButton bCancel = new JFXButton("Anuluj");
        JFXButton bOkay = new JFXButton("Zgłoś");
        AlertMaker.showMaterialDialog(spMain, apMain, Arrays.asList(bOkay,bCancel), "Zgłoś zapotrzebowanie", "Czy na pewno chcesz zgłosić zapotrzebowanie na wybrany produkt?");
        
        bOkay.setOnAction((ActionEvent event1) -> {
            try {
                Connection conn = DBConnection.Connect();
                   
                String tresc = "Na stanie brakuje produktu : " + pr.getNazwa_produktu() + " ID [" + pr.getId_produktu() + "]";
                LocalDate ld = LocalDate.now();
                conn.createStatement().executeUpdate("INSERT INTO wiadomosci (id_wiadomosci,id_pracownika_nadawcy,id_pracownika_odbiorca,temat_wiadomosci,tresc_wiadomosci,Data,status_wiadomosci) "
                        + "VALUES (null,"+FXMLPracownikController.idPracownika+","+FXMLPracownikController.idKierownika+",'Na sklepie brakuje produktu.','"+tresc+"','"+ld.toString()+"','Nieodebrana');");
                
                JFXButton bOkay2 = new JFXButton("OK");
                AlertMaker.showMaterialDialog(spMain, apMain, Arrays.asList(bOkay2), "Zgłoszenie przesłane.", "Proces zgłoszenia przebiegł pomyślnie. Wiadomość o brakach została przekazana do kierownika oddziału.");
        
        
        conn.close();
            } catch (Exception exp) {
                //nieobsłużone
                //System.out.println(exp);
            }
        });
        
        
        
        
        
    }

    /**
     * Otworzenie okna umożliwiającego złożenie nowego zamówienia.
     * @param event
     * @throws IOException 
     */
    @FXML
    private void zamowNowyProdukt(ActionEvent event) throws IOException {
       
        Stage stage;
        Parent root;

        stage = new Stage();
        root = FXMLLoader.load(getClass().getResource("FXMLDodajNowyProdukt.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("Panel zamawiania produktu");
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(bNowyProdukt.getScene().getWindow());
        stage.showAndWait();

    }

    /**
     * Przypisanie do obiektu klasy Produkt wybranego obiektu z tabeli za pomocą kursora.
     * @param event 
     */
    @FXML
    private void zaznaczProdukt(MouseEvent event) {
        
        if (tableProdukty.getSelectionModel().getSelectedItem() != null) pr = tableProdukty.getSelectionModel().getSelectedItem();
        
    }

}
