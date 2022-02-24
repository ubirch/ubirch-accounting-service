const { monthlyReportTable } = require('./monthlyReportTable')
const { Request } = require('./getter')
const yargs = require('yargs/yargs')
const { hideBin } = require('yargs/helpers')
const argv = yargs(hideBin(process.argv)).argv

const config = require('./config.json')

function logAndExit () {
  console.log('Usage -> monthlyReport.js --id=UUID_OF_IDENTITY --cat=CATEGORY --date=YYYY-MM --subcat=SUBCATEGORY')
  process.exit(1)
}

if (argv.id === undefined || argv.cat === undefined || argv.date === undefined) {
  logAndExit()
}

if (argv.date) {
  const dateParts = argv.date.split('-')
  if (dateParts.length === 2) {
    argv.date = dateParts[0] + '-' + dateParts[1] + '-' + '01'
  } else if (dateParts.length === 3) {
    argv.date = dateParts[0] + '-' + dateParts[1] + '-' + dateParts[2]
  } else {
    logAndExit()
  }
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
