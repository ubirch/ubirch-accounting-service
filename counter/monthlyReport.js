const { monthlyReportTable } = require('./monthlyReportTable')
const { Request } = require('./getter')

const config = require('./config.json')
const request = new Request(
  config.host,
  config.token,
  '12539f76-c7e9-47d6-b37b-4b59380721ac',
  'verification',
  '2022-02-02', // The day element is ignored for the byMonth function
  null // subcategory (tag), e.g: entry-a
)

monthlyReportTable(request)
