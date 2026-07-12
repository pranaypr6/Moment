using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Moment.Api.Migrations
{
    /// <inheritdoc />
    public partial class AddCompoundIndexes : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateIndex(
                name: "IX_Moments_ReceiverUserId_Status",
                table: "Moments",
                columns: new[] { "ReceiverUserId", "Status" });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_Moments_ReceiverUserId_Status",
                table: "Moments");
        }
    }
}
