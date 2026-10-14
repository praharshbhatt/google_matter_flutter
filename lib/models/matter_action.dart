/// The set of cluster commands/reads the native side (`GoogleMatter.performClusterAction` in
/// Kotlin) knows how to execute. [wireValue] must exactly match the `action` string switched on
/// in the Kotlin implementation.
enum MatterAction {
  getOnOff('getOnOff'),
  toggleOnOff('toggleOnOff'),
  getLevel('getLevel'),
  setLevel('setLevel'),
  setHueAndSaturation('setHueAndSaturation'),
  setColorTemperature('setColorTemperature'),
  lockDoor('lockDoor'),
  unlockDoor('unlockDoor'),
  getLockState('getLockState'),
  openWindowCovering('openWindowCovering'),
  closeWindowCovering('closeWindowCovering'),
  setWindowCoveringLiftPosition('setWindowCoveringLiftPosition'),
  getThermostatLocalTemperature('getThermostatLocalTemperature'),
  setThermostatHeatingSetpoint('setThermostatHeatingSetpoint'),
  setThermostatCoolingSetpoint('setThermostatCoolingSetpoint'),
  getTemperatureMeasurement('getTemperatureMeasurement'),
  getRelativeHumidityMeasurement('getRelativeHumidityMeasurement'),
  getIlluminanceMeasurement('getIlluminanceMeasurement'),
  getPressureMeasurement('getPressureMeasurement'),
  getFlowMeasurement('getFlowMeasurement'),
  getOccupancy('getOccupancy'),
  getContactState('getContactState');

  final String wireValue;

  const MatterAction(this.wireValue);
}
