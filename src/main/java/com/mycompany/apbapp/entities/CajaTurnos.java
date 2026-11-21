package com.mycompany.apbapp.entities;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "caja_turnos")
public class CajaTurnos implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;

    @Basic(optional = false)
    @NotNull
    @Column(name = "created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Basic(optional = false)
    @NotNull
    @Column(name = "modified")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modified;

    @Basic(optional = false)
    @NotNull
    @Column(name = "enabled")
    private boolean enabled = true;

    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Users usuario;

    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_apertura")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaApertura;

    @Column(name = "fecha_cierre")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCierre;

    @Basic(optional = false)
    @NotNull
    @Column(name = "monto_inicial")
    private int montoInicial;

    @Basic(optional = false)
    @NotNull
    @Column(name = "total_ventas_efectivo")
    private int totalVentasEfectivo;

    @Basic(optional = false)
    @NotNull
    @Column(name = "total_ventas_debito")
    private int totalVentasDebito;

    @Basic(optional = false)
    @NotNull
    @Column(name = "total_ventas_credito")
    private int totalVentasCredito;

    @Basic(optional = false)
    @NotNull
    @Column(name = "total_ventas_transferencia")
    private int totalVentasTransferencia;

    @Basic(optional = false)
    @NotNull
    @Column(name = "monto_esperado_cierre")
    private int montoEsperadoCierre;

    @Column(name = "monto_real_cierre")
    private Integer montoRealCierre;

    @Column(name = "diferencia_cierre")
    private Integer diferenciaCierre;

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "estado")
    private String estado = "ABIERTA";

    @Size(max = 255)
    @Column(name = "observacion")
    private String observacion;

    public CajaTurnos() {
    }

    public CajaTurnos(Integer id) {
        this.id = id;
    }

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        if (this.created == null) {
            this.created = now;
        }
        if (this.fechaApertura == null) {
            this.fechaApertura = now;
        }
        this.modified = now;
        recalcularEsperado();
    }

    @PreUpdate
    protected void onUpdate() {
        this.modified = new Date();
        recalcularEsperado();
    }

    public void recalcularEsperado() {
        this.montoEsperadoCierre = this.montoInicial + this.totalVentasEfectivo;
    }

    public int getTotalVentasGeneral() {
        return this.totalVentasEfectivo
                + this.totalVentasDebito
                + this.totalVentasCredito
                + this.totalVentasTransferencia;
    }

    public boolean isAbierta() {
        return "ABIERTA".equalsIgnoreCase(this.estado);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Users getUsuario() {
        return usuario;
    }

    public void setUsuario(Users usuario) {
        this.usuario = usuario;
    }

    public Date getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(Date fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public Date getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(Date fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public int getMontoInicial() {
        return montoInicial;
    }

    public void setMontoInicial(int montoInicial) {
        this.montoInicial = montoInicial;
    }

    public int getTotalVentasEfectivo() {
        return totalVentasEfectivo;
    }

    public void setTotalVentasEfectivo(int totalVentasEfectivo) {
        this.totalVentasEfectivo = totalVentasEfectivo;
    }

    public int getTotalVentasDebito() {
        return totalVentasDebito;
    }

    public void setTotalVentasDebito(int totalVentasDebito) {
        this.totalVentasDebito = totalVentasDebito;
    }

    public int getTotalVentasCredito() {
        return totalVentasCredito;
    }

    public void setTotalVentasCredito(int totalVentasCredito) {
        this.totalVentasCredito = totalVentasCredito;
    }

    public int getTotalVentasTransferencia() {
        return totalVentasTransferencia;
    }

    public void setTotalVentasTransferencia(int totalVentasTransferencia) {
        this.totalVentasTransferencia = totalVentasTransferencia;
    }

    public int getMontoEsperadoCierre() {
        return montoEsperadoCierre;
    }

    public void setMontoEsperadoCierre(int montoEsperadoCierre) {
        this.montoEsperadoCierre = montoEsperadoCierre;
    }

    public Integer getMontoRealCierre() {
        return montoRealCierre;
    }

    public void setMontoRealCierre(Integer montoRealCierre) {
        this.montoRealCierre = montoRealCierre;
    }

    public Integer getDiferenciaCierre() {
        return diferenciaCierre;
    }

    public void setDiferenciaCierre(Integer diferenciaCierre) {
        this.diferenciaCierre = diferenciaCierre;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CajaTurnos)) {
            return false;
        }
        CajaTurnos other = (CajaTurnos) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "com.mycompany.apbapp.entities.CajaTurnos[ id=" + id + " ]";
    }
}
