const { monthlyReportTable } = require('./monthlyReportTable')
const { Request } = require('./getter')
const yargs = require('yargs/yargs')
const { hideBin } = require('yargs/helpers')
const argv = yargs(hideBin(process.argv)).argv

const config = require('./config.json')

if (argv.id === undefined || argv.cat === undefined || argv.date === undefined) {
  console.log('Usage -> monthlyReport.js --id=UUID_OF_IDENTITY --cat=CATEGORY --date=YYYY-MM-DD --subcat=SUBCATEGORY')
  process.exit(1)
}

const request = new Request(
  config.host,
  config.token,
  argv.id,
  argv.cat,
  argv.date,
  argv.subcat
)

monthlyReportTable(request)
