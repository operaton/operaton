import { GET } from '../helper.jsx'

export const get_telemetry_data = (state) =>
  GET('/engine/default/telemetry/data', state, state.api.engine.telemetry)

const engine =
  {
    telemetry: get_telemetry_data
  }

export default engine